package com.car.rental.api.dto;

/**
 * Full employee registration: ZK user + fingerprint enroll + database row.
 */
public class RegisterEmployeeRequest {

    /** Optional; if omitted server assigns next id from 1001. */
    private String deviceUserId;

    /** English full name (required for ZK). */
    private String name;

    private String phone;

    private String driverLicenseNumber;
    private String driverLicenseExpiry;

    /**
     * Finger index 0–9 (ZKTeco standard):
     * 0=Right Thumb .. 4=Right Pinky, 5=Left Thumb .. 9=Left Pinky.
     */
    private int fingerIndex = 0;

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDriverLicenseNumber() {
        return driverLicenseNumber;
    }

    public void setDriverLicenseNumber(String driverLicenseNumber) {
        this.driverLicenseNumber = driverLicenseNumber;
    }

    public String getDriverLicenseExpiry() {
        return driverLicenseExpiry;
    }

    public void setDriverLicenseExpiry(String driverLicenseExpiry) {
        this.driverLicenseExpiry = driverLicenseExpiry;
    }

    public int getFingerIndex() {
        return fingerIndex;
    }

    public void setFingerIndex(int fingerIndex) {
        this.fingerIndex = fingerIndex;
    }
}
