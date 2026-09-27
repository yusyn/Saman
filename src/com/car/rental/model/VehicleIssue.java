package com.car.rental.model;

/**
 * A reported damage / defect / issue on a fleet vehicle.
 */
public class VehicleIssue {

    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CLOSED = "CLOSED";

    private final int id;
    private int vehicleId;
    private Integer reportedBy;       // employee_id (nullable)
    private String title;
    private String description;
    private String severity;
    private String status;
    private double cost;
    private String reportedAt;
    private String resolvedAt;
    private String notes;
    private String createdAt;

    /** New issue (not yet persisted). */
    public VehicleIssue(int vehicleId, Integer reportedBy, String title, String description,
                        String severity, String status, double cost,
                        String reportedAt, String notes) {
        this.id = 0;
        this.vehicleId = vehicleId;
        this.reportedBy = reportedBy;
        this.title = title;
        this.description = description;
        this.severity = normalizeSeverity(severity);
        this.status = normalizeStatus(status);
        this.cost = cost;
        this.reportedAt = reportedAt;
        this.notes = notes;
    }

    /** Full constructor when loading from database. */
    public VehicleIssue(int id, int vehicleId, Integer reportedBy, String title, String description,
                        String severity, String status, double cost,
                        String reportedAt, String resolvedAt, String notes, String createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.reportedBy = reportedBy;
        this.title = title;
        this.description = description;
        this.severity = normalizeSeverity(severity);
        this.status = normalizeStatus(status);
        this.cost = cost;
        this.reportedAt = reportedAt;
        this.resolvedAt = resolvedAt;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static String normalizeSeverity(String s) {
        if (s == null || s.isBlank()) {
            return SEVERITY_MEDIUM;
        }
        String t = s.strip().toUpperCase();
        switch (t) {
            case SEVERITY_LOW:
            case "کم":
                return SEVERITY_LOW;
            case SEVERITY_HIGH:
            case "زیاد":
            case "بالا":
                return SEVERITY_HIGH;
            case SEVERITY_CRITICAL:
            case "بحرانی":
                return SEVERITY_CRITICAL;
            default:
                return SEVERITY_MEDIUM;
        }
    }

    public static String normalizeStatus(String s) {
        if (s == null || s.isBlank()) {
            return STATUS_OPEN;
        }
        String t = s.strip().toUpperCase();
        switch (t) {
            case STATUS_IN_PROGRESS:
            case "INPROGRESS":
            case "در حال تعمیر":
                return STATUS_IN_PROGRESS;
            case STATUS_RESOLVED:
            case "رفع شده":
                return STATUS_RESOLVED;
            case STATUS_CLOSED:
            case "بسته":
                return STATUS_CLOSED;
            default:
                return STATUS_OPEN;
        }
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status) || STATUS_IN_PROGRESS.equals(status);
    }

    public int getId() {
        return id;
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
        this.severity = normalizeSeverity(severity);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = normalizeStatus(status);
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
}
