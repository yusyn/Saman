package com.car.rental.api.dto;

import com.car.rental.model.Employee;
import com.car.rental.service.VerificationResult;

import java.time.LocalDateTime;

public class VerificationResponse {

    private String deviceUserId;
    private String deviceTime;
    private int verifyType;
    private String name;
    private String phone;
    private boolean renting;

    public VerificationResponse() {
    }

    public static VerificationResponse from(VerificationResult r) {
        VerificationResponse d = new VerificationResponse();
        if (r == null) {
            return d;
        }
        d.deviceUserId = r.getDeviceUserId();
        LocalDateTime t = r.getDeviceTime();
        d.deviceTime = t != null ? t.toString() : null;
        d.verifyType = r.getVerifyType();
        return d;
    }

    /** Attach employee profile fields from DB (name/phone/renting). */
    public void applyEmployee(Employee e) {
        if (e == null) {
            return;
        }
        if (e.getDeviceUserId() != null && !e.getDeviceUserId().isBlank()) {
            this.deviceUserId = e.getDeviceUserId();
        }
        this.name = e.getName();
        this.phone = e.getPhone();
        this.renting = e.isRenting();
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public String getDeviceTime() {
        return deviceTime;
    }

    public void setDeviceTime(String deviceTime) {
        this.deviceTime = deviceTime;
    }

    public int getVerifyType() {
        return verifyType;
    }

    public void setVerifyType(int verifyType) {
        this.verifyType = verifyType;
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

    public boolean isRenting() {
        return renting;
    }

    public void setRenting(boolean renting) {
        this.renting = renting;
    }
}
