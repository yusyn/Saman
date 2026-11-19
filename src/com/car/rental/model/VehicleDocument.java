package com.car.rental.model;

public class VehicleDocument {
    private final int id;
    private final int vehicleId;
    private final String documentType; // THIRD_PARTY_INSURANCE, BODY_INSURANCE, TECHNICAL_INSPECTION, OTHER
    private final String documentNumber;
    private final String issueDate;
    private final String expiryDate;
    private final Integer attachmentId;
    private final String notes;
    private final String createdAt;

    public VehicleDocument(int id, int vehicleId, String documentType, String documentNumber,
                           String issueDate, String expiryDate, Integer attachmentId,
                           String notes, String createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.attachmentId = attachmentId;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getVehicleId() { return vehicleId; }
    public String getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public String getIssueDate() { return issueDate; }
    public String getExpiryDate() { return expiryDate; }
    public Integer getAttachmentId() { return attachmentId; }
    public String getNotes() { return notes; }
    public String getCreatedAt() { return createdAt; }
}
