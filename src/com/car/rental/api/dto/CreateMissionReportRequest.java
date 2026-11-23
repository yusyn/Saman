package com.car.rental.api.dto;

public class CreateMissionReportRequest {
    private String reportType; // FUEL, SERVICE, ACCIDENT, FINE
    private String title;
    private String description;
    private Double amount;
    private Double cost;
    private Double liters;
    private String severity;
    private String fineType;
    private Integer attachmentId;
    private String reportDate;

    public CreateMissionReportRequest() {}

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getAmount() {
        if (cost != null) return cost;
        return amount;
    }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getCost() {
        return cost != null ? cost : amount;
    }
    public void setCost(Double cost) { this.cost = cost; }
    public Double getLiters() {
        return liters;
    }
    public void setLiters(Double liters) { this.liters = liters; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getFineType() { return fineType; }
    public void setFineType(String fineType) { this.fineType = fineType; }
    public Integer getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Integer attachmentId) { this.attachmentId = attachmentId; }
    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }
}
