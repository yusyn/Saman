package com.car.rental.api;

import com.car.rental.api.dto.OkResponse;
import com.car.rental.api.dto.VehicleDocumentDto;
import com.car.rental.db.VehicleDocumentRepository;
import com.car.rental.db.VehicleRepository;
import com.car.rental.model.VehicleDocument;
import com.car.rental.service.VehicleHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles/{plate}/documents")
public class VehicleDocumentController {

    private final VehicleDocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleHistoryService historyService;

    public VehicleDocumentController(VehicleDocumentRepository documentRepository,
                                     VehicleRepository vehicleRepository,
                                     VehicleHistoryService historyService) {
        this.documentRepository = documentRepository;
        this.vehicleRepository = vehicleRepository;
        this.historyService = historyService;
    }

    @GetMapping
    public List<VehicleDocumentDto> list(@PathVariable("plate") String plate) throws SQLException {
        VehicleRepository.VehicleRow row = requireVehicle(plate);
        return documentRepository.findByVehicleId(row.id).stream()
                .map(VehicleDocumentDto::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDocumentDto addDocument(@PathVariable("plate") String plate,
                                          @RequestBody VehicleDocumentDto body) throws SQLException {
        VehicleRepository.VehicleRow row = requireVehicle(plate);
        if (body == null || body.getDocumentType() == null || body.getExpiryDate() == null) {
            throw new IllegalArgumentException("نوع مدرک و تاریخ انقضا الزامی است");
        }
        VehicleDocument doc = new VehicleDocument(
                0,
                row.id,
                body.getDocumentType().toUpperCase().trim(),
                body.getDocumentNumber() != null ? body.getDocumentNumber().trim() : "",
                body.getIssueDate() != null ? body.getIssueDate().trim() : "",
                body.getExpiryDate().trim(),
                body.getAttachmentId(),
                body.getNotes(),
                null
        );
        int id = documentRepository.insert(doc);
        body.setId(id);
        body.setVehicleId(row.id);
        return body;
    }

    @DeleteMapping("/{docId}")
    public OkResponse deleteDocument(@PathVariable("plate") String plate,
                                     @PathVariable("docId") int docId) throws SQLException {
        requireVehicle(plate);
        documentRepository.delete(docId);
        return OkResponse.ok("مدرک حذف شد");
    }

    private VehicleRepository.VehicleRow requireVehicle(String plate) throws SQLException {
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("پلاک الزامی است");
        }
        VehicleRepository.VehicleRow row = vehicleRepository.findRowByPlate(plate.strip());
        if (row == null) {
            throw new SQLException("وسیله‌ای با این پلاک یافت نشد: " + plate);
        }
        return row;
    }
}
