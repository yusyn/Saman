package com.car.rental.api.dto;

import com.car.rental.model.VehicleIssue;

/** JSON contract for a vehicle issue / damage record over HTTP. */
public class VehicleIssueDto {

    private int id;
    private int vehicleId;
    private Integer reportedBy;
    private String title;
    private String description;
    private String severity;
    private String status;
    private double cost;
    private String reportedAt;
    private String resolvedAt;
    private String notes;
    private String createdAt;

    public VehicleIssueDto() {
    }

    public static VehicleIssueDto from(VehicleIssue i) {
        if (i == null) {
            return null;
        }
        VehicleIssueDto d = new VehicleIssueDto();
        d.id = i.getId();
        d.vehicleId = i.getVehicleId();
        d.reportedBy = i.getReportedBy();
        d.title = i.getTitle();
        d.description = i.getDescription();
        d.severity = i.getSeverity();
        d.status = i.getStatus();
        d.cost = i.getCost();
        d.reportedAt = i.getReportedAt();
        d.resolvedAt = i.getResolvedAt();
        d.notes = i.getNotes();
        d.createdAt = i.getCreatedAt();
        return d;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(Integer reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(String reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(String resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
