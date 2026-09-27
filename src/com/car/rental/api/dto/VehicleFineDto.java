package com.car.rental.api.dto;

import com.car.rental.model.VehicleFine;

/** JSON contract for a vehicle fine / violation record over HTTP. */
public class VehicleFineDto {

    private int id;
    private int vehicleId;
    private Integer employeeId;
    private String fineType;
    private double amount;
    private String fineDate;
    private String dueDate;
    private boolean paid;
    private String paymentDate;
    private String description;
    private String createdAt;

    public VehicleFineDto() {
    }

    public static VehicleFineDto from(VehicleFine f) {
        if (f == null) {
            return null;
        }
        VehicleFineDto d = new VehicleFineDto();
        d.id = f.getId();
        d.vehicleId = f.getVehicleId();
        d.employeeId = f.getEmployeeId();
        d.fineType = f.getFineType();
        d.amount = f.getAmount();
        d.fineDate = f.getFineDate();
        d.dueDate = f.getDueDate();
        d.paid = f.isPaid();
        d.paymentDate = f.getPaymentDate();
        d.description = f.getDescription();
        d.createdAt = f.getCreatedAt();
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
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

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
