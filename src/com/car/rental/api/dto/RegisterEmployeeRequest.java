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

    /**
     * Finger index 0–9:
     * left little=0 … left thumb=4, right thumb=5 … right little=9.
     */
    private int fingerIndex = 5;

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

    public int getFingerIndex() {
        return fingerIndex;
    }

    public void setFingerIndex(int fingerIndex) {
        this.fingerIndex = fingerIndex;
    }
}
