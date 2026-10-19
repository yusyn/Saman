package com.car.rental.api.dto;

/**
 * Operational counts for the dashboard's active fleet.
 */
public class FleetDashboardSummaryDto {

    private final int totalCars;
    private final int totalMotorcycles;
    private final int availableCars;
    private final int availableMotorcycles;
    private final int onMissionCars;
    private final int onMissionMotorcycles;
    private final int inRepairCars;
    private final int inRepairMotorcycles;

    public FleetDashboardSummaryDto(int totalCars, int totalMotorcycles,
                                    int availableCars, int availableMotorcycles,
                                    int onMissionCars, int onMissionMotorcycles,
                                    int inRepairCars, int inRepairMotorcycles) {
        this.totalCars = totalCars;
        this.totalMotorcycles = totalMotorcycles;
        this.availableCars = availableCars;
        this.availableMotorcycles = availableMotorcycles;
        this.onMissionCars = onMissionCars;
        this.onMissionMotorcycles = onMissionMotorcycles;
        this.inRepairCars = inRepairCars;
        this.inRepairMotorcycles = inRepairMotorcycles;
    }

    public int getTotal() {
        return totalCars + totalMotorcycles;
    }

    public int getAvailable() {
        return availableCars + availableMotorcycles;
    }

    public int getOnMission() {
        return onMissionCars + onMissionMotorcycles;
    }

    public int getInRepair() {
        return inRepairCars + inRepairMotorcycles;
    }

    public int getTotalCars() {
        return totalCars;
    }

    public int getTotalMotorcycles() {
        return totalMotorcycles;
    }

    public int getAvailableCars() {
        return availableCars;
    }

    public int getAvailableMotorcycles() {
        return availableMotorcycles;
    }

    public int getOnMissionCars() {
        return onMissionCars;
    }

    public int getOnMissionMotorcycles() {
        return onMissionMotorcycles;
    }

    public int getInRepairCars() {
        return inRepairCars;
    }

    public int getInRepairMotorcycles() {
        return inRepairMotorcycles;
    }
}
