package com.car.rental.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated history dossier for a single fleet vehicle:
 * usage (rentals), services, issues, fines, costs and status badges.
 */
public class VehicleHistoryDto {

    private VehicleDto vehicle;
    private int vehicleId;
    private int currentOdometer;
    private Integer lastServiceOdometer;
    private String lastServiceDate;
    private int totalRentals;
    private double totalServiceCost;
    private double totalFineAmount;
    private double unpaidFineAmount;
    private List<String> statusBadges = new ArrayList<>();
    private List<RentalRecordDto> rentals = new ArrayList<>();
    private List<VehicleServiceDto> services = new ArrayList<>();
    private List<VehicleIssueDto> issues = new ArrayList<>();
    private List<VehicleFineDto> fines = new ArrayList<>();

    public VehicleHistoryDto() {
    }

    public VehicleDto getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleDto vehicle) {
        this.vehicle = vehicle;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getCurrentOdometer() {
        return currentOdometer;
    }

    public void setCurrentOdometer(int currentOdometer) {
        this.currentOdometer = currentOdometer;
    }

    public Integer getLastServiceOdometer() {
        return lastServiceOdometer;
    }

    public void setLastServiceOdometer(Integer lastServiceOdometer) {
        this.lastServiceOdometer = lastServiceOdometer;
    }

    public String getLastServiceDate() {
        return lastServiceDate;
    }

    public void setLastServiceDate(String lastServiceDate) {
        this.lastServiceDate = lastServiceDate;
    }

    public int getTotalRentals() {
        return totalRentals;
    }

    public void setTotalRentals(int totalRentals) {
        this.totalRentals = totalRentals;
    }

    public double getTotalServiceCost() {
        return totalServiceCost;
    }

    public void setTotalServiceCost(double totalServiceCost) {
        this.totalServiceCost = totalServiceCost;
    }

    public double getTotalFineAmount() {
        return totalFineAmount;
    }

    public void setTotalFineAmount(double totalFineAmount) {
        this.totalFineAmount = totalFineAmount;
    }

    public double getUnpaidFineAmount() {
        return unpaidFineAmount;
    }

    public void setUnpaidFineAmount(double unpaidFineAmount) {
        this.unpaidFineAmount = unpaidFineAmount;
    }

    public List<String> getStatusBadges() {
        return statusBadges;
    }

    public void setStatusBadges(List<String> statusBadges) {
        this.statusBadges = statusBadges != null ? statusBadges : new ArrayList<>();
    }

    public List<RentalRecordDto> getRentals() {
        return rentals;
    }

    public void setRentals(List<RentalRecordDto> rentals) {
        this.rentals = rentals != null ? rentals : new ArrayList<>();
    }

    public List<VehicleServiceDto> getServices() {
        return services;
    }

    public void setServices(List<VehicleServiceDto> services) {
        this.services = services != null ? services : new ArrayList<>();
    }

    public List<VehicleIssueDto> getIssues() {
        return issues;
    }

    public void setIssues(List<VehicleIssueDto> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
    }

    public List<VehicleFineDto> getFines() {
        return fines;
    }

    public void setFines(List<VehicleFineDto> fines) {
        this.fines = fines != null ? fines : new ArrayList<>();
    }
}
