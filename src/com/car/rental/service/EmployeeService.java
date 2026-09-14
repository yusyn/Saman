package com.car.rental.service;

import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import com.car.rental.util.InputValidators;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Employees: coordinates fingerprint device and database.
 * Registration order: ZK enroll success → DB insert; on DB failure → best-effort device rollback.
 */
@Service
public class EmployeeService {

    private static final Logger logger = Logger.getLogger(EmployeeService.class.getName());

    private final DatabaseManager db;
    private final FingerprintService fingerprintService;
    private final FingerprintDeviceGate deviceGate;

    public EmployeeService(DatabaseManager db,
                           FingerprintService fingerprintService,
                           FingerprintDeviceGate deviceGate) {
        this.db = db;
        this.fingerprintService = fingerprintService;
        this.deviceGate = deviceGate;
    }

    public String getNextDeviceUserId() throws SQLException {
        return db.getNextDeviceUserId();
    }

    public boolean isDeviceUserIdExists(String deviceUserId) throws SQLException {
        return db.isDeviceUserIdExists(deviceUserId);
    }

    public Employee findByDeviceUserId(String deviceUserId) throws SQLException {
        return db.findByDeviceUserId(deviceUserId);
    }

    public List<Employee> getAllEmployees() throws SQLException {
        return db.getAllEmployees();
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
            deviceUserId = db.getNextDeviceUserId();
        } else {
            deviceUserId = requestedDeviceUserId.strip();
            if (db.isDeviceUserIdExists(deviceUserId)) {
                throw new SQLException("شناسه کارمند قبلاً ثبت شده است: " + deviceUserId);
            }
        }

        try {
            deviceGate.call(() -> {
                ensureConnected();
                try {
                    fingerprintService.registerUserWithFingerprint(deviceUserId, finalName, fingerIndex);
                    try {
                        db.addEmployee(deviceUserId, finalName, finalPhone);
                    } catch (SQLException dbEx) {
                        logger.log(Level.SEVERE,
                                "DB insert failed after ZK enroll; rolling back device user " + deviceUserId,
                                dbEx);
                        try {
                            fingerprintService.deleteUser(deviceUserId);
                        } catch (Exception delEx) {
                            logger.log(Level.SEVERE, "Device rollback failed for " + deviceUserId, delEx);
                        }
                        throw dbEx;
                    }
                } finally {
                    disconnectQuietly();
                }
                return null;
            });
        } catch (SQLException | FingerprintException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            rethrowDeviceOrSql(e);
            throw new FingerprintException("ثبت کارمند ناموفق بود", e);
        }

        Employee saved = db.findByDeviceUserId(deviceUserId);
        if (saved == null) {
            throw new SQLException("کارمند بعد از ثبت یافت نشد: " + deviceUserId);
        }
        return saved;
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

        try {
            deviceGate.call(() -> {
                ensureConnected();
                try {
                    fingerprintService.updateUserName(emp.getDeviceUserId(), name);
                    db.updateEmployee(emp);
                } finally {
                    disconnectQuietly();
                }
                return null;
            });
        } catch (SQLException | FingerprintException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            rethrowDeviceOrSql(e);
            throw new FingerprintException("به‌روزرسانی کارمند ناموفق بود", e);
        }
    }

    public void addFingerprint(String deviceUserId, int fingerIndex)
            throws FingerprintException, SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کارمند خالی است");
        }
        validateFingerIndex(fingerIndex);
        if (!db.isDeviceUserIdExists(deviceUserId)) {
            throw new IllegalArgumentException("کارمند در دیتابیس نیست: " + deviceUserId);
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
            throw e;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            rethrowDeviceOrSql(e);
            throw new FingerprintException("ثبت انگشت اضافه ناموفق بود", e);
        }
    }

    public void deleteEmployee(String deviceUserId) throws SQLException {
        try {
            deviceGate.call(() -> {
                try {
                    ensureConnected();
                    fingerprintService.deleteUser(deviceUserId);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Device deleteUser failed for " + deviceUserId, e);
                } finally {
                    disconnectQuietly();
                }
                return null;
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Device gate during delete", e);
        }
        db.deleteEmployeeByDeviceUserId(deviceUserId);
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
