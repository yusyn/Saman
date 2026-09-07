package com.car.rental.api.dto;

import com.car.rental.model.Car;

/** JSON contract for cars over HTTP (stable for remote clients). */
public class CarDto {

    private String name;
    private String plate;
    private String color;
    private String status;

    public CarDto() {
    }

    public CarDto(String name, String plate, String color, String status) {
        this.name = name;
        this.plate = plate;
        this.color = color;
        this.status = status;
    }

    public static CarDto from(Car car) {
        if (car == null) {
            return null;
        }
        return new CarDto(car.getModel(), car.getPlate(), car.getColor(), car.getStatus());
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
}
