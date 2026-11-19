package com.car.rental.model;

public class Attachment {
    private final int id;
    private final String originalName;
    private final String filePath;
    private final String contentType;
    private final long fileSize;
    private final String createdAt;

    public Attachment(int id, String originalName, String filePath, String contentType, long fileSize, String createdAt) {
        this.id = id;
        this.originalName = originalName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public Attachment(String originalName, String filePath, String contentType, long fileSize) {
        this(0, originalName, filePath, contentType, fileSize, null);
    }

    public int getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getFilePath() { return filePath; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public String getCreatedAt() { return createdAt; }
}
