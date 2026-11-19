package com.car.rental.api.dto;

import com.car.rental.model.VehicleMissionReport;

public class VehicleMissionReportDto {
    private int id;
    private Integer rentalId;
    private int vehicleId;
    private Integer employeeId;
    private String employeeName;
    private String reportType;
    private String title;
    private String description;
    private double amount;
    private double liters;
    private String severity;
    private String fineType;
    private boolean paid;
    private Integer attachmentId;
    private String attachmentUrl;
    private String reportDate;
    private String createdAt;

    public VehicleMissionReportDto() {}

    public static VehicleMissionReportDto from(VehicleMissionReport r) {
        if (r == null) return null;
        VehicleMissionReportDto dto = new VehicleMissionReportDto();
        dto.setId(r.getId());
        dto.setRentalId(r.getRentalId());
        dto.setVehicleId(r.getVehicleId());
        dto.setEmployeeId(r.getEmployeeId());
        dto.setReportType(r.getReportType());
        dto.setTitle(r.getTitle());
        dto.setDescription(r.getDescription());
        dto.setAmount(r.getAmount());
        dto.setLiters(r.getLiters());
        dto.setSeverity(r.getSeverity());
        dto.setFineType(r.getFineType());
        dto.setPaid(r.isPaid());
        dto.setAttachmentId(r.getAttachmentId());
        if (r.getAttachmentId() != null && r.getAttachmentId() > 0) {
            dto.setAttachmentUrl("/api/attachments/" + r.getAttachmentId());
        }
        dto.setReportDate(r.getReportDate());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getRentalId() { return rentalId; }
    public void setRentalId(Integer rentalId) { this.rentalId = rentalId; }
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getLiters() { return liters; }
    public void setLiters(double liters) { this.liters = liters; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getFineType() { return fineType; }
    public void setFineType(String fineType) { this.fineType = fineType; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public Integer getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Integer attachmentId) { this.attachmentId = attachmentId; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
