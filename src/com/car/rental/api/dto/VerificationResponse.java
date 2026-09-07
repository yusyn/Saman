package com.car.rental.api.dto;

import com.car.rental.service.VerificationResult;

import java.time.LocalDateTime;

public class VerificationResponse {

    private String deviceUserId;
    private String deviceTime;
    private int verifyType;

    public VerificationResponse() {
    }

    public static VerificationResponse from(VerificationResult r) {
        VerificationResponse d = new VerificationResponse();
        d.deviceUserId = r.getDeviceUserId();
        LocalDateTime t = r.getDeviceTime();
        d.deviceTime = t != null ? t.toString() : null;
        d.verifyType = r.getVerifyType();
        return d;
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
}
