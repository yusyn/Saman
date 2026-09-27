package com.car.rental.model;

/**
 * A traffic fine / violation recorded against a fleet vehicle
 * (and optionally the employee who was driving).
 */
public class VehicleFine {

    public static final String TYPE_SPEED = "SPEED";
    public static final String TYPE_PARKING = "PARKING";
    public static final String TYPE_RED_LIGHT = "RED_LIGHT";
    public static final String TYPE_OTHER = "OTHER";

    private final int id;
    private int vehicleId;
    private Integer employeeId;       // driver responsible (nullable)
    private String fineType;
    private double amount;
    private String fineDate;
    private String dueDate;
    private boolean paid;
    private String paymentDate;
    private String description;
    private String createdAt;

    /** New fine (not yet persisted). */
    public VehicleFine(int vehicleId, Integer employeeId, String fineType, double amount,
                       String fineDate, String dueDate, String description) {
        this.id = 0;
        this.vehicleId = vehicleId;
        this.employeeId = employeeId;
        this.fineType = normalizeType(fineType);
        this.amount = amount;
        this.fineDate = fineDate;
        this.dueDate = dueDate;
        this.paid = false;
        this.description = description;
    }

    /** Full constructor when loading from database. */
    public VehicleFine(int id, int vehicleId, Integer employeeId, String fineType, double amount,
                       String fineDate, String dueDate, boolean paid, String paymentDate,
                       String description, String createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.employeeId = employeeId;
        this.fineType = normalizeType(fineType);
        this.amount = amount;
        this.fineDate = fineDate;
        this.dueDate = dueDate;
        this.paid = paid;
        this.paymentDate = paymentDate;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return TYPE_OTHER;
        }
        String t = type.strip().toUpperCase();
        switch (t) {
            case TYPE_SPEED:
            case "سرعت":
                return TYPE_SPEED;
            case TYPE_PARKING:
            case "پارک":
            case "توقف":
                return TYPE_PARKING;
            case TYPE_RED_LIGHT:
            case "چراغ قرمز":
            case "RED":
                return TYPE_RED_LIGHT;
            default:
                return TYPE_OTHER;
        }
    }

    public int getId() {
        return id;
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
        this.fineType = normalizeType(fineType);
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
}
