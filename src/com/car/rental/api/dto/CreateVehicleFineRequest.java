package com.car.rental.api.dto;

/** Request body for registering a new vehicle fine / violation. */
public class CreateVehicleFineRequest {

    /** Optional internal employee id of the driver. */
    private Integer employeeId;
    /** SPEED, PARKING, RED_LIGHT, OTHER */
    private String fineType;
    private Double amount;
    private String fineDate;
    private String dueDate;
    private String description;

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getFineType() {
        return fineType;
    }

    public void setFineType(String fineType) {
        this.fineType = fineType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getFineDate() {
        return fineDate;
    }

    public void setFineDate(String fineDate) {
        this.fineDate = fineDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
