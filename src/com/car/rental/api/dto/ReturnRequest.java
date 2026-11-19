package com.car.rental.api.dto;

import java.util.List;

/** Body for POST /api/rentals/return */
public class ReturnRequest {

    private String deviceUserId;
    /** Mandatory return odometer reading */
    private Integer returnOdometer;
    /** Optional return timestamp string */
    private String returnTime;
    /** Optional reports (fuel, service, accident, fine) logged upon return */
    private List<CreateMissionReportRequest> reports;

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public Integer getReturnOdometer() {
        return returnOdometer;
    }

    public void setReturnOdometer(Integer returnOdometer) {
        this.returnOdometer = returnOdometer;
    }

    public String getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(String returnTime) {
        this.returnTime = returnTime;
    }

    public List<CreateMissionReportRequest> getReports() {
        return reports;
    }

    public void setReports(List<CreateMissionReportRequest> reports) {
        this.reports = reports;
    }
}
