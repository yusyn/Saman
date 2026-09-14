package com.car.rental.api.dto;

/** Body for POST /api/rentals/pickup */
public class PickupRequest {

    private String deviceUserId;
    private String plate;
    private String destination;
    /** Optional; Jalali or device time string as used by the app, e.g. 1405/05/25 10:00:00 */
    private String pickupTime;

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(String pickupTime) {
        this.pickupTime = pickupTime;
    }
}
