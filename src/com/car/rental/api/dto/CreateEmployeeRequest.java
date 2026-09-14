package com.car.rental.api.dto;

/**
 * Creates employee in the application DB only (no ZK enroll).
 * For full fingerprint registration a later endpoint will call the device.
 */
public class CreateEmployeeRequest {

    /** If null/blank, server assigns next id (from 1001). */
    private String deviceUserId;
    private String name;
    private String phone;

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
}
