package com.car.rental.service;

import com.car.rental.db.EmployeeRepository;
import com.car.rental.model.Employee;
import com.car.rental.util.InputValidators;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Employees: coordinates fingerprint device and database.
 * Registration order: ZK enroll success → DB insert; on DB failure → best-effort device rollback.
 */
@Service
public class EmployeeService {

    private static final Logger logger = Logger.getLogger(EmployeeService.class.getName());

    private final EmployeeRepository employees;
    private final FingerprintService fingerprintService;
    private final FingerprintDeviceGate deviceGate;
    private final SystemEventService systemEvents;
    private final DeviceOperationService deviceOperations;

    public EmployeeService(EmployeeRepository employees,
                           FingerprintService fingerprintService,
                           FingerprintDeviceGate deviceGate,
                           SystemEventService systemEvents,
                           DeviceOperationService deviceOperations) {
        this.employees = employees;
        this.fingerprintService = fingerprintService;
        this.deviceGate = deviceGate;
        this.systemEvents = systemEvents;
        this.deviceOperations = deviceOperations;
    }

    public String getNextDeviceUserId() throws SQLException {
        return employees.getNextDeviceUserId();
    }

    public boolean isDeviceUserIdExists(String deviceUserId) throws SQLException {
        return employees.isDeviceUserIdExists(deviceUserId);
    }

    public Employee findByDeviceUserId(String deviceUserId) throws SQLException {
        return employees.findByDeviceUserId(deviceUserId);
    }

    public List<Employee> getAllEmployees() throws SQLException {
        return employees.getAllEmployees();
    }

    public Employee registerWithFingerprint(String requestedDeviceUserId,
                                            String name,
                                            String phone,
                                            int fingerIndex)
            throws SQLException, FingerprintException {

        String nameError = InputValidators.validateEnglishFullName(name);
        if (nameError != null) {
            throw new IllegalArgumentException(nameError);
        }
        final String finalName = name.strip();
        final String finalPhone = phone != null ? phone : "";
        validateFingerIndex(fingerIndex);

        final String deviceUserId;
        if (requestedDeviceUserId == null || requestedDeviceUserId.isBlank()) {
            deviceUserId = employees.getNextDeviceUserId();
        } else {
            deviceUserId = requestedDeviceUserId.strip();
            if (employees.isDeviceUserIdExists(deviceUserId)) {
                throw new SQLException("شناسه کارمند قبلاً ثبت شده است: " + deviceUserId);
            }
        }

        // Drop a stale verify listen so we do not wait forever on the shared device gate.
        try {
            fingerprintService.cancelListen();
        } catch (Exception ignored) {
        }

        String operationId=deviceOperations.begin("ثبت کارمند و اثر انگشت","کارمند",deviceUserId,
                Map.of("name",finalName,"phone",finalPhone,"fingerIndex",fingerIndex));
        try {
            deviceGate.call(() -> {
                ensureConnected();
                try { fingerprintService.registerUserWithFingerprint(deviceUserId, finalName, fingerIndex); }
                finally { disconnectQuietly(); }
                return null;
            });
        } catch (Exception e) {
            Throwable cause=unwrap(e);
            deviceOperations.deviceFailed(operationId,"دستگاه اثرانگشت","ثبت کارمند روی دستگاه","ثبت کارمند روی دستگاه انجام نشد",cause);
            throw new FingerprintException("ثبت کارمند روی دستگاه ناموفق بود",cause);
        }
        deviceOperations.deviceSucceeded(operationId);
        Map<String,?> details=Map.of("new",Map.of("name",finalName,"phone",finalPhone),"operationId",operationId);
        try {
            return systemEvents.inTransaction(
                    new SystemEventService.EventSpec("کارمندان","ثبت کارمند","کارمند ثبت شد: "+finalName,"کارمند",deviceUserId,details),
                    new SystemEventService.EventSpec("کارمندان","ثبت کارمند","ثبت کارمند پس از عملیات دستگاه انجام نشد","کارمند",deviceUserId,Map.of("operationId",operationId)),
                    conn->{employees.addEmployee(conn,deviceUserId,finalName,finalPhone);deviceOperations.complete(conn,operationId);return employees.findByDeviceUserId(conn,deviceUserId);});
        } catch(SQLException dbFailure) {
            boolean compensated=false;
            try { deviceGate.call(()->{ensureConnected();try{fingerprintService.deleteUser(deviceUserId);}finally{disconnectQuietly();}return null;}); compensated=true; }
            catch(Exception compensationFailure){dbFailure.addSuppressed(compensationFailure);logger.log(Level.SEVERE,"Unable to compensate device enrollment for operation "+operationId,compensationFailure);}
            deviceOperations.markAfterCompensation(operationId,compensated,dbFailure);
            throw new DeviceOperationException(compensated
                    ? "ثبت پایگاه‌داده انجام نشد و کاربر دستگاه حذف شد؛ عملیات کامل نشده است. شناسهٔ پیگیری "+operationId
                    : "کاربر در دستگاه ثبت شده اما ذخیرهٔ پایگاه‌داده شکست خورد و جبران کامل نشد. تکرار نکنید؛ شناسهٔ پیگیری "+operationId,
                    operationId,compensated?"FAILED":"RECONCILIATION_REQUIRED",compensated?500:202,true,dbFailure);
        }
    }

    public void updateEmployee(Employee emp) throws SQLException, FingerprintException {
        if (emp == null) {
            throw new IllegalArgumentException("employee is null");
        }
        String nameError = InputValidators.validateEnglishFullName(emp.getName());
        if (nameError != null) {
            throw new IllegalArgumentException(nameError);
        }
        final String name = emp.getName().strip();
        Employee before = employees.findByDeviceUserId(emp.getDeviceUserId());
        if(before==null)throw new SQLException("کارمند برای ویرایش یافت نشد");

        try {
            fingerprintService.cancelListen();
        } catch (Exception ignored) {
        }

        String operationId=deviceOperations.begin("ویرایش نام کارمند و دستگاه","کارمند",emp.getDeviceUserId(),
                Map.of("oldName",before.getName(),"newName",name,"newPhone",emp.getPhone()==null?"":emp.getPhone()));
        try {
            deviceGate.call(() -> {
                ensureConnected();
                try {
                    fingerprintService.updateUserName(emp.getDeviceUserId(), name);
                } finally {
                    disconnectQuietly();
                }
                return null;
            });
        } catch (Exception e) {
            Throwable cause=unwrap(e);
            deviceOperations.deviceFailed(operationId,"دستگاه اثرانگشت","ویرایش نام کارمند روی دستگاه","ویرایش نام کارمند روی دستگاه انجام نشد",cause);
            throw new FingerprintException("ویرایش نام روی دستگاه ناموفق بود",cause);
        }
        deviceOperations.deviceSucceeded(operationId);
        Map<String,?> details=Map.of("old",Map.of("name",before.getName(),"phone",before.getPhone()==null?"":before.getPhone()),
                "new",Map.of("name",name,"phone",emp.getPhone()==null?"":emp.getPhone()),"operationId",operationId);
        try {
            systemEvents.inTransaction(
                    new SystemEventService.EventSpec("کارمندان","ویرایش کارمند","مشخصات کارمند ویرایش شد","کارمند",emp.getDeviceUserId(),details),
                    new SystemEventService.EventSpec("کارمندان","ویرایش کارمند","ویرایش پایگاه‌داده پس از تغییر دستگاه انجام نشد","کارمند",emp.getDeviceUserId(),Map.of("operationId",operationId)),
                    conn->{employees.updateEmployee(conn,emp);deviceOperations.complete(conn,operationId);return null;});
        } catch(SQLException dbFailure) {
            boolean compensated=false;
            try {deviceGate.call(()->{ensureConnected();try{fingerprintService.updateUserName(emp.getDeviceUserId(),before.getName());}finally{disconnectQuietly();}return null;});compensated=true;}
            catch(Exception compensationFailure){dbFailure.addSuppressed(compensationFailure);logger.log(Level.SEVERE,"Unable to compensate employee device rename for operation "+operationId,compensationFailure);}
            deviceOperations.markAfterCompensation(operationId,compensated,dbFailure);
            throw new DeviceOperationException(compensated
                    ?"ذخیرهٔ مشخصات کارمند انجام نشد و نام دستگاه به مقدار قبلی برگشت؛ شناسهٔ پیگیری "+operationId
                    :"نام کارمند روی دستگاه تغییر کرده اما پایگاه‌داده به‌روز نشد و جبران کامل نشد. تکرار نکنید؛ شناسهٔ پیگیری "+operationId,
                    operationId,compensated?"FAILED":"RECONCILIATION_REQUIRED",compensated?500:202,true,dbFailure);
        }
    }

    public void addFingerprint(String deviceUserId, int fingerIndex)
            throws FingerprintException, SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کارمند خالی است");
        }
        validateFingerIndex(fingerIndex);
        if (!employees.isDeviceUserIdExists(deviceUserId)) {
            throw new IllegalArgumentException("کارمند در دیتابیس نیست: " + deviceUserId);
        }
        String operationId=deviceOperations.begin("افزودن اثر انگشت","کارمند",deviceUserId,Map.of("fingerIndex",fingerIndex));

        try {
            fingerprintService.cancelListen();
        } catch (Exception ignored) {
        }

        try {
            deviceGate.call(() -> {
                ensureConnected();
                try {
                    fingerprintService.enrollFingerOnly(deviceUserId, fingerIndex);
                } finally {
                    disconnectQuietly();
                }
                return null;
            });
        } catch (FingerprintException | IllegalArgumentException e) {
            deviceOperations.deviceFailed(operationId,"دستگاه اثرانگشت","ثبت اثر انگشت","ثبت اثر انگشت ناموفق بود",e);
            throw e;
        } catch (SQLException e) {
            deviceOperations.deviceFailed(operationId,"دستگاه اثرانگشت","ثبت اثر انگشت","ثبت اثر انگشت ناموفق بود",e);
            throw e;
        } catch (Exception e) {
            deviceOperations.deviceFailed(operationId,"دستگاه اثرانگشت","ثبت اثر انگشت","عملیات ثبت اثر انگشت روی دستگاه ناموفق بود",unwrap(e));
            throw new FingerprintException("ثبت انگشت اضافه ناموفق بود", e);
        }
        deviceOperations.deviceSucceeded(operationId);
        deviceOperations.completeDeviceOnly(operationId,"دستگاه اثرانگشت","ثبت اثر انگشت","اثر انگشت برای کارمند ثبت شد","کارمند",deviceUserId);
    }

    public void deleteEmployee(String deviceUserId) throws SQLException {
        Employee before = employees.findByDeviceUserId(deviceUserId);
        if(before==null)throw new SQLException("کارمند برای حذف یافت نشد");
        String operationId=deviceOperations.begin("حذف کارمند و حساب دستگاه","کارمند",deviceUserId,
                Map.of("name",before.getName(),"phone",before.getPhone()==null?"":before.getPhone()));
        Map<String,?> details=Map.of("old",Map.of("name",before.getName(),"phone",before.getPhone()==null?"":before.getPhone()),
                "operationId",operationId);
        try {
            systemEvents.inTransaction(
                    new SystemEventService.EventSpec("کارمندان","حذف کارمند","کارمند از پایگاه‌داده حذف شد","کارمند",deviceUserId,details),
                    new SystemEventService.EventSpec("کارمندان","حذف کارمند","حذف کارمند انجام نشد","کارمند",deviceUserId,Map.of("operationId",operationId)),
                    conn->{employees.deleteEmployeeByDeviceUserId(conn,deviceUserId);deviceOperations.databaseCommitted(conn,operationId);return null;});
        } catch(SQLException dbFailure) {
            deviceOperations.failBeforeDevice(operationId,dbFailure);
            throw dbFailure;
        }

        try {
            deviceGate.call(() -> {
                ensureConnected();
                try { fingerprintService.deleteUser(deviceUserId); }
                finally { disconnectQuietly(); }
                return null;
            });
        } catch(Exception deviceFailure) {
            Throwable cause=unwrap(deviceFailure);
            logger.log(Level.WARNING,"Device cleanup failed for operation "+operationId,cause);
            throw deviceOperations.cleanupFailed(operationId,"کارمند",deviceUserId,cause);
        }
        try {
            deviceOperations.completeAfterDatabase(operationId);
        } catch(SQLException ledgerFailure) {
            deviceOperations.markReconciliation(operationId,true,"حذف دستگاه انجام شد اما وضعیت نهایی دفتر عملیات ثبت نشد",ledgerFailure);
            systemEvents.recordFailureSafely(new SystemEventService.EventSpec("دستگاه اثرانگشت","ثبت وضعیت حذف دستگاه",
                    "حذف دستگاه انجام شد اما ثبت وضعیت نهایی نیازمند پیگیری است","کارمند",deviceUserId,
                    Map.of("operationId",operationId,"operationState","RECONCILIATION_REQUIRED")),ledgerFailure);
            throw new DeviceOperationException("کارمند از پایگاه‌داده و دستگاه حذف شد اما ثبت وضعیت نهایی کامل نشد. حذف را تکرار نکنید؛ شناسهٔ پیگیری "+operationId,
                    operationId,"RECONCILIATION_REQUIRED",202,true,ledgerFailure);
        }
    }

    private void ensureConnected() throws FingerprintException {
        if (!fingerprintService.isConnected()) {
            fingerprintService.connect();
        }
    }

    private void disconnectQuietly() {
        try {
            fingerprintService.disconnect();
        } catch (Exception ignored) {
        }
    }

    private static void validateFingerIndex(int fingerIndex) {
        if (fingerIndex < 0 || fingerIndex > 9) {
            throw new IllegalArgumentException("ایندکس انگشت باید بین 0 و 9 باشد");
        }
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current;
    }

    /** Prefer the original checked exception when the gate wraps it. */
    private static void rethrowDeviceOrSql(Exception e) throws SQLException, FingerprintException {
        Throwable c = e;
        while (c != null) {
            if (c instanceof SQLException) {
                throw (SQLException) c;
            }
            if (c instanceof FingerprintException) {
                throw (FingerprintException) c;
            }
            if (c instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) c;
            }
            c = c.getCause();
        }
    }
}
