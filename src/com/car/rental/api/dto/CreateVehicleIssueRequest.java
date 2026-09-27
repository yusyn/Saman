package com.car.rental.api.dto;

/** Request body for reporting a new vehicle issue / damage. */
public class CreateVehicleIssueRequest {

    /** Optional internal employee id who reported the issue. */
    private Integer reportedBy;
    private String title;
    private String description;
    /** LOW, MEDIUM, HIGH, CRITICAL */
    private String severity;
    private Double cost;
    private String reportedAt;
    private String notes;

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

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public String getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(String reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
