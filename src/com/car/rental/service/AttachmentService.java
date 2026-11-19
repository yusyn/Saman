package com.car.rental.service;

import com.car.rental.db.AttachmentRepository;
import com.car.rental.model.Attachment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository repository;
    private final Path uploadDir = Paths.get("data", "uploads").toAbsolutePath().normalize();

    public AttachmentService(AttachmentRepository repository) {
        this.repository = repository;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create upload dir: " + uploadDir, e);
        }
    }

    public Attachment store(MultipartFile file) throws IOException, SQLException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("فایل خالی است");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "upload.bin";
        }
        // Normalize filename to prevent path traversal
        originalName = Paths.get(originalName).getFileName().toString();

        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot);
        }
        String storedName = UUID.randomUUID() + ext;
        Path target = uploadDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new SecurityException("مسیر فایل نامعتبر است");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Attachment attachment = new Attachment(
                originalName,
                target.toString(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize()
        );
        return repository.insert(attachment);
    }

    public Attachment getAttachment(int id) throws SQLException {
        return repository.findById(id);
    }

    public Resource loadAsResource(Attachment attachment) {
        if (attachment == null) return null;
        Path path = Paths.get(attachment.getFilePath()).normalize();
        if (Files.exists(path) && Files.isReadable(path)) {
            return new FileSystemResource(path);
        }
        return null;
    }
}
