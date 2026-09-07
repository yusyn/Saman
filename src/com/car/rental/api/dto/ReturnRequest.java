package com.car.rental.api.dto;

/** Body for POST /api/rentals/return */
public class ReturnRequest {

    private String deviceUserId;
    /** Optional return timestamp string */
    private String returnTime;

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public String getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(String returnTime) {
        this.returnTime = returnTime;
    }
}
