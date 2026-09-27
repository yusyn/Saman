package com.car.rental.api.dto;

import com.car.rental.model.VehicleServiceRecord;

/** JSON contract for a vehicle service record over HTTP. */
public class VehicleServiceDto {

    private int id;
    private int vehicleId;
    private String serviceType;
    private String description;
    private Integer odometer;
    private double cost;
    private String serviceDate;
    private Integer nextDueOdometer;
    private String nextDueDate;
    private String performedBy;
    private String notes;
    private String createdAt;

    public VehicleServiceDto() {
    }

    public static VehicleServiceDto from(VehicleServiceRecord r) {
        if (r == null) {
            return null;
        }
        VehicleServiceDto d = new VehicleServiceDto();
        d.id = r.getId();
        d.vehicleId = r.getVehicleId();
        d.serviceType = r.getServiceType();
        d.description = r.getDescription();
        d.odometer = r.getOdometer();
        d.cost = r.getCost();
        d.serviceDate = r.getServiceDate();
        d.nextDueOdometer = r.getNextDueOdometer();
        d.nextDueDate = r.getNextDueDate();
        d.performedBy = r.getPerformedBy();
        d.notes = r.getNotes();
        d.createdAt = r.getCreatedAt();
        return d;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(String serviceDate) {
        this.serviceDate = serviceDate;
    }

    public Integer getNextDueOdometer() {
        return nextDueOdometer;
    }

    public void setNextDueOdometer(Integer nextDueOdometer) {
        this.nextDueOdometer = nextDueOdometer;
    }

    public String getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(String nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
