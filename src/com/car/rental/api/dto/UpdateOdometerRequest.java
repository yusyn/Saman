package com.car.rental.api.dto;

/** Request body for updating a vehicle's current odometer reading. */
public class UpdateOdometerRequest {

    private Integer odometer;

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }
}
