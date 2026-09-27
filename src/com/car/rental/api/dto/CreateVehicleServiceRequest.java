package com.car.rental.api.dto;

/** Request body for registering a new vehicle service / maintenance event. */
public class CreateVehicleServiceRequest {

    /** OIL_CHANGE, FILTER, TIRE, BATTERY, BRAKE, INSPECTION, OTHER */
    private String serviceType;
    private String description;
    private Integer odometer;
    private Double cost;
    private String serviceDate;
    private Integer nextDueOdometer;
    private String nextDueDate;
    private String performedBy;
    private String notes;

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

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
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
}
