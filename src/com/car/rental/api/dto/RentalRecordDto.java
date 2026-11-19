package com.car.rental.api.dto;

import com.car.rental.model.RentalRecord;

public class RentalRecordDto {

    private int id;
    private String deviceUserId;
    private String employeeName;
    private String carName;
    private String carColor;
    private String plate;
    private String pickupDate;
    private String returnDate;
    private String destination;
    private String vehicleType;
    private Integer returnOdometer;
    private Integer currentOdometer;
    private java.util.List<VehicleMissionReportDto> reports = new java.util.ArrayList<>();

    public RentalRecordDto() {
    }

    public static RentalRecordDto from(RentalRecord r) {
        if (r == null) {
            return null;
        }
        RentalRecordDto d = new RentalRecordDto();
        d.id = r.id;
        d.deviceUserId = r.deviceUserId;
        d.employeeName = r.employeeName;
        d.carName = r.carName;
        d.carColor = r.carColor;
        d.plate = r.plate;
        d.pickupDate = r.pickupDate;
        d.returnDate = r.returnDate;
        d.destination = r.destination;
        d.vehicleType = r.vehicleType != null ? r.vehicleType : "CAR";
        d.returnOdometer = r.returnOdometer;
        d.currentOdometer = r.currentOdometer;
        return d;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getReturnOdometer() {
        return returnOdometer;
    }

    public void setReturnOdometer(Integer returnOdometer) {
        this.returnOdometer = returnOdometer;
    }

    public java.util.List<VehicleMissionReportDto> getReports() {
        return reports;
    }

    public void setReports(java.util.List<VehicleMissionReportDto> reports) {
        this.reports = reports != null ? reports : new java.util.ArrayList<>();
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

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getCurrentOdometer() {
        return currentOdometer;
    }

    public void setCurrentOdometer(Integer currentOdometer) {
        this.currentOdometer = currentOdometer;
    }
}
