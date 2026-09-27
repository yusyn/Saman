package com.car.rental.api.dto;

import com.car.rental.model.Vehicle;

/** JSON contract for vehicles over HTTP. */
public class VehicleDto {

    private String name;
    private String plate;
    private String color;
    private String status;
    private String vehicleType;
    /** Comma-separated history badges e.g. DAMAGED,HAS_FINE */
    private String statusFlags;

    public VehicleDto() {
    }

    public VehicleDto(String name, String plate, String color, String status, String vehicleType) {
        this(name, plate, color, status, vehicleType, null);
    }

    public VehicleDto(String name, String plate, String color, String status, String vehicleType, String statusFlags) {
        this.name = name;
        this.plate = plate;
        this.color = color;
        this.status = status;
        this.vehicleType = vehicleType;
        this.statusFlags = statusFlags;
    }

    public static VehicleDto from(Vehicle v) {
        if (v == null) {
            return null;
        }
        return new VehicleDto(
                v.getModel(),
                v.getPlate(),
                v.getColor(),
                v.getStatus(),
                v.getVehicleType(),
                v.getStatusFlags()
        );
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
        this.vehicleType = vehicleType;
    }

    public String getStatusFlags() {
        return statusFlags;
    }

    public void setStatusFlags(String statusFlags) {
        this.statusFlags = statusFlags;
    }
}
