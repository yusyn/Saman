package com.car.rental.api.dto;

/** Optional body for POST /api/fingerprint/verify */
public class VerifyRequest {

    /** Seconds to wait for a finger; null = use server default from config */
    private Integer timeoutSeconds;

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
