package com.car.rental.api.dto;

/** Current rental shown in the operational dashboard. */
public class ActiveRentalDto {

    private final String employeeName;
    private final String vehicleName;
    private final String plate;
    private final String destination;
    private final String pickupDate;
    private final String vehicleType;
    private final Long elapsedMinutes;

    public ActiveRentalDto(String employeeName, String vehicleName, String plate,
                           String destination, String pickupDate, String vehicleType,
                           Long elapsedMinutes) {
        this.employeeName = employeeName;
        this.vehicleName = vehicleName;
        this.plate = plate;
        this.destination = destination;
        this.pickupDate = pickupDate;
        this.vehicleType = vehicleType;
        this.elapsedMinutes = elapsedMinutes;
    }

    public String getEmployeeName() { return employeeName; }
    public String getVehicleName() { return vehicleName; }
    public String getPlate() { return plate; }
    public String getDestination() { return destination; }
    public String getPickupDate() { return pickupDate; }
    public String getVehicleType() { return vehicleType; }
    public Long getElapsedMinutes() { return elapsedMinutes; }
}
