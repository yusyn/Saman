package com.car.rental.api.dto;

import com.car.rental.model.RentalRecord;

public class RentalRecordDto {

    private String deviceUserId;
    private String employeeName;
    private String carName;
    private String carColor;
    private String plate;
    private String pickupDate;
    private String returnDate;
    private String destination;

    public RentalRecordDto() {
    }

    public static RentalRecordDto from(RentalRecord r) {
        if (r == null) {
            return null;
        }
        RentalRecordDto d = new RentalRecordDto();
        d.deviceUserId = r.deviceUserId;
        d.employeeName = r.employeeName;
        d.carName = r.carName;
        d.carColor = r.carColor;
        d.plate = r.plate;
        d.pickupDate = r.pickupDate;
        d.returnDate = r.returnDate;
        d.destination = r.destination;
        return d;
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }

    public String getCarColor() {
        return carColor;
    }

    public void setCarColor(String carColor) {
        this.carColor = carColor;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(String pickupDate) {
        this.pickupDate = pickupDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
