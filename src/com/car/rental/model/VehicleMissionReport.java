package com.car.rental.model;

public class VehicleMissionReport {
    private final int id;
    private final Integer rentalId;
    private final int vehicleId;
    private final Integer employeeId;
    private final String reportType; // FUEL, SERVICE, ACCIDENT, FINE
    private final String title;
    private final String description;
    private final double amount;
    private final double liters;
    private final String severity;
    private final String fineType;
    private final boolean paid;
    private final Integer attachmentId;
    private final String reportDate;
    private final String createdAt;

    public VehicleMissionReport(int id, Integer rentalId, int vehicleId, Integer employeeId,
                                String reportType, String title, String description,
                                double amount, double liters, String severity,
                                String fineType, boolean paid, Integer attachmentId,
                                String reportDate, String createdAt) {
        this.id = id;
        this.rentalId = rentalId;
        this.vehicleId = vehicleId;
        this.employeeId = employeeId;
        this.reportType = reportType;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.liters = liters;
        this.severity = severity;
        this.fineType = fineType;
        this.paid = paid;
        this.attachmentId = attachmentId;
        this.reportDate = reportDate;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public Integer getRentalId() { return rentalId; }
    public int getVehicleId() { return vehicleId; }
    public Integer getEmployeeId() { return employeeId; }
    public String getReportType() { return reportType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public double getLiters() { return liters; }
    public String getSeverity() { return severity; }
    public String getFineType() { return fineType; }
    public boolean isPaid() { return paid; }
    public Integer getAttachmentId() { return attachmentId; }
    public String getReportDate() { return reportDate; }
    public String getCreatedAt() { return createdAt; }
}
