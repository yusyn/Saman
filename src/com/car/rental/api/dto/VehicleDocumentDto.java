package com.car.rental.api.dto;

import com.car.rental.model.VehicleDocument;

public class VehicleDocumentDto {
    private int id;
    private int vehicleId;
    private String documentType;
    private String documentNumber;
    private String issueDate;
    private String expiryDate;
    private Integer attachmentId;
    private String attachmentUrl;
    private String notes;
    private String createdAt;

    public VehicleDocumentDto() {}

    public static VehicleDocumentDto from(VehicleDocument doc) {
        if (doc == null) return null;
        VehicleDocumentDto dto = new VehicleDocumentDto();
        dto.setId(doc.getId());
        dto.setVehicleId(doc.getVehicleId());
        dto.setDocumentType(doc.getDocumentType());
        dto.setDocumentNumber(doc.getDocumentNumber());
        dto.setIssueDate(doc.getIssueDate());
        dto.setExpiryDate(doc.getExpiryDate());
        dto.setAttachmentId(doc.getAttachmentId());
        if (doc.getAttachmentId() != null && doc.getAttachmentId() > 0) {
            dto.setAttachmentUrl("/api/attachments/" + doc.getAttachmentId());
        }
        dto.setNotes(doc.getNotes());
        dto.setCreatedAt(doc.getCreatedAt());
        return dto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public Integer getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Integer attachmentId) { this.attachmentId = attachmentId; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
