package com.car.rental.api.dto;

/**
 * Update car fields. {@code oldPlate} identifies the row (primary key in practice is plate).
 */
public class UpdateCarRequest {

    private String oldPlate;
    private String name;
    private String plate;
    private String color;

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
}
