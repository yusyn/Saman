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

    /**
     * Full registration: allocate id if needed → enroll on ZK → save DB.
     * Device work runs under {@link FingerprintDeviceGate}.
     */
    public Employee registerWithFingerprint(String requestedDeviceUserId,
                                            String name,
                                            String phone,
                                            int fingerIndex)
            throws SQLException, FingerprintException {

        String nameError = InputValidators.validateEnglishFullName(name);
        if (nameError != null) {
            throw new IllegalArgumentException(nameError);
        }
        name = name.strip();
        if (phone == null) {
            phone = "";
        }
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

        final String finalName = name;
        final String finalPhone = phone;

        try {
            deviceGate.run(() -> {
                ensureConnected();
                try {
                    fingerprintService.registerUserWithFingerprint(deviceUserId, finalName, fingerIndex);
                    try {
                        db.addEmployee(deviceUserId, finalName, finalPhone);
                    } catch (SQLException dbEx) {
                        logger.log(Level.SEVERE, "DB insert failed after ZK enroll; rolling back device user "
                                + deviceUserId, dbEx);
                        try {
                            fingerprintService.deleteUser(deviceUserId);
                        } catch (Exception delEx) {
                            logger.log(Level.SEVERE,
                                    "Device rollback failed for " + deviceUserId, delEx);
                        }
                        throw dbEx;
                    }
                } finally {
                    disconnectQuietly();
                }
            });
        } catch (SQLException | FingerprintException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            if (e.getCause() instanceof FingerprintException) {
                throw (FingerprintException) e.getCause();
            }
            throw new FingerprintException(
                    e.getMessage() != null ? e.getMessage() : "ثبت کارمند ناموفق بود", e);
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

        try {
            deviceGate.run(() -> {
                ensureConnected();
                try {
                    fingerprintService.updateUserName(emp.getDeviceUserId(), emp.getName().strip());
                    db.updateEmployee(emp);
                } finally {
                    disconnectQuietly();
                }
            });
        } catch (SQLException | FingerprintException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            unwrapAndThrow(e);
        }
    }

    public void addFingerprint(String deviceUserId, int fingerIndex) throws FingerprintException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کارمند خالی است");
        }
        validateFingerIndex(fingerIndex);
        try {
            if (!db.isDeviceUserIdExists(deviceUserId)) {
                throw new IllegalArgumentException("کارمند در دیتابیس نیست: " + deviceUserId);
            }
        } catch (SQLException e) {
            throw new FingerprintException("خطا در خواندن دیتابیس", e);
        }

        try {
            deviceGate.run(() -> {
                ensureConnected();
                try {
                    fingerprintService.enrollFingerOnly(deviceUserId, fingerIndex);
                } finally {
                    disconnectQuietly();
                }
            });
        } catch (FingerprintException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            unwrapAndThrow(e);
        }
    }

    public void deleteEmployee(String deviceUserId) throws SQLException {
        try {
            deviceGate.run(() -> {
                try {
                    ensureConnected();
                    fingerprintService.deleteUser(deviceUserId);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Device deleteUser failed for " + deviceUserId, e);
                } finally {
                    disconnectQuietly();
                }
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

    private static void unwrapAndThrow(Exception e) throws FingerprintException, SQLException {
        Throwable c = e.getCause() != null ? e.getCause() : e;
        if (c instanceof FingerprintException) {
            throw (FingerprintException) c;
        }
        if (c instanceof SQLException) {
            throw (SQLException) c;
        }
        if (c instanceof IllegalArgumentException) {
            throw (IllegalArgumentException) c;
        }
        throw new FingerprintException(c.getMessage() != null ? c.getMessage() : "خطای دستگاه", c);
    }
}
