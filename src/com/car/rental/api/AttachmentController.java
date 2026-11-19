package com.car.rental.api;

import com.car.rental.api.dto.AttachmentDto;
import com.car.rental.model.Attachment;
import com.car.rental.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentDto upload(@RequestParam("file") MultipartFile file) throws IOException, SQLException {
        Attachment saved = attachmentService.store(file);
        return AttachmentDto.from(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") int id) throws SQLException {
        Attachment attachment = attachmentService.getAttachment(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = attachmentService.loadAsResource(attachment);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(attachment.getContentType());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalName() + "\"")
                .body(resource);
    }
}
