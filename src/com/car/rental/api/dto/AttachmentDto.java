package com.car.rental.api.dto;

import com.car.rental.model.Attachment;

public class AttachmentDto {
    private int id;
    private String originalName;
    private String contentType;
    private long fileSize;
    private String url;
    private String createdAt;

    public AttachmentDto() {}

    public static AttachmentDto from(Attachment a) {
        if (a == null) return null;
        AttachmentDto dto = new AttachmentDto();
        dto.setId(a.getId());
        dto.setOriginalName(a.getOriginalName());
        dto.setContentType(a.getContentType());
        dto.setFileSize(a.getFileSize());
        dto.setUrl("/api/attachments/" + a.getId());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
