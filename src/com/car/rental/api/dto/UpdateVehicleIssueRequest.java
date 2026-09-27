package com.car.rental.api.dto;

/** Request body for updating an existing vehicle issue (status, cost, notes, ...). */
public class UpdateVehicleIssueRequest {

    /** OPEN, IN_PROGRESS, RESOLVED, CLOSED */
    private String status;
    private Double cost;
    private String resolvedAt;
    private String notes;
    private String severity;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
