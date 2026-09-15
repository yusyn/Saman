package com.car.rental.model;

/**
 * Fleet vehicle (car or motorcycle).
 */
public class Vehicle {

    public static final String TYPE_CAR = "CAR";
    public static final String TYPE_MOTORCYCLE = "MOTORCYCLE";

    private String model;
    private String plate;
    private String color;
    private String status;
    /** CAR or MOTORCYCLE */
    private String vehicleType;

    /** Standard order: model, plate, color, vehicleType */
    public Vehicle(String model, String plate, String color, String vehicleType) {
        this.model = model;
        this.plate = plate;
        this.color = color;
        this.vehicleType = normalizeType(vehicleType);
    }

    public Vehicle(String model, String plate, String color, String vehicleType, String status) {
        this(model, plate, color, vehicleType);
        this.status = status;
    }

    public static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return TYPE_CAR;
        }
        String t = type.strip().toUpperCase();
        if (TYPE_MOTORCYCLE.equals(t) || "MOTOR".equals(t) || "BIKE".equals(t)) {
            return TYPE_MOTORCYCLE;
        }
        return TYPE_CAR;
    }

    public boolean isMotorcycle() {
        return TYPE_MOTORCYCLE.equals(vehicleType);
    }

    public boolean isCar() {
        return !isMotorcycle();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = normalizeType(vehicleType);
    }

    /** Display text for combo boxes and lists (not used for parsing). */
    @Override
    public String toString() {
        String m = model != null ? model : "";
        String c = color != null ? color : "";
        String p = plate != null ? plate : "";
        return m + " - " + c + " - " + p;
    }
}
