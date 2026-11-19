package com.car.rental.api;

import com.car.rental.api.dto.AttachmentDto;
import com.car.rental.model.Attachment;
import com.car.rental.service.AttachmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AttachmentServiceTest {

    @Autowired
    private AttachmentService attachmentService;

    @Test
    void testUploadAndRetrieve() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                "image/png",
                "test-image-content".getBytes()
        );
        Attachment att = attachmentService.store(file);
        assertNotNull(att);
        assertTrue(att.getId() > 0);
        assertEquals("receipt.png", att.getOriginalName());

        Attachment loaded = attachmentService.getAttachment(att.getId());
        assertNotNull(loaded);
        assertEquals(att.getId(), loaded.getId());
        assertNotNull(attachmentService.loadAsResource(loaded));
    }
}
