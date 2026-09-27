package com.car.rental.model;

/**
 * A single maintenance / service event for a fleet vehicle
 * (oil change, filter, tire, battery, etc.).
 */
public class VehicleServiceRecord {

    public static final String TYPE_OIL_CHANGE = "OIL_CHANGE";
    public static final String TYPE_FILTER = "FILTER";
    public static final String TYPE_TIRE = "TIRE";
    public static final String TYPE_BATTERY = "BATTERY";
    public static final String TYPE_BRAKE = "BRAKE";
    public static final String TYPE_INSPECTION = "INSPECTION";
    public static final String TYPE_OTHER = "OTHER";

    private final int id;
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

    /** New record (not yet persisted). */
    public VehicleServiceRecord(int vehicleId, String serviceType, String description,
                                Integer odometer, double cost, String serviceDate,
                                Integer nextDueOdometer, String nextDueDate,
                                String performedBy, String notes) {
        this.id = 0;
        this.vehicleId = vehicleId;
        this.serviceType = normalizeType(serviceType);
        this.description = description;
        this.odometer = odometer;
        this.cost = cost;
        this.serviceDate = serviceDate;
        this.nextDueOdometer = nextDueOdometer;
        this.nextDueDate = nextDueDate;
        this.performedBy = performedBy;
        this.notes = notes;
    }

    /** Full constructor when loading from database. */
    public VehicleServiceRecord(int id, int vehicleId, String serviceType, String description,
                                Integer odometer, double cost, String serviceDate,
                                Integer nextDueOdometer, String nextDueDate,
                                String performedBy, String notes, String createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.serviceType = normalizeType(serviceType);
        this.description = description;
        this.odometer = odometer;
        this.cost = cost;
        this.serviceDate = serviceDate;
        this.nextDueOdometer = nextDueOdometer;
        this.nextDueDate = nextDueDate;
        this.performedBy = performedBy;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return TYPE_OTHER;
        }
        String t = type.strip().toUpperCase();
        switch (t) {
            case TYPE_OIL_CHANGE:
            case "OIL":
            case "روغن":
                return TYPE_OIL_CHANGE;
            case TYPE_FILTER:
            case "فیلتر":
                return TYPE_FILTER;
            case TYPE_TIRE:
            case "TYRE":
            case "لاستیک":
                return TYPE_TIRE;
            case TYPE_BATTERY:
            case "باتری":
                return TYPE_BATTERY;
            case TYPE_BRAKE:
            case "ترمز":
                return TYPE_BRAKE;
            case TYPE_INSPECTION:
            case "معاینه":
            case "بازرسی":
                return TYPE_INSPECTION;
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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = normalizeType(serviceType);
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
}
