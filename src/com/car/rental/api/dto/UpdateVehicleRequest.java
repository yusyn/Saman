package com.car.rental.api.dto;

/**
 * Update vehicle fields. {@code oldPlate} identifies the row.
 */
public class UpdateVehicleRequest {

    private String oldPlate;
    private String name;
    private String plate;
    private String color;
    private String vehicleType;

    public String getOldPlate() {
        return oldPlate;
    }

    public void setOldPlate(String oldPlate) {
        this.oldPlate = oldPlate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}
