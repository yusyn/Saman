package com.car.rental.api;

import com.car.rental.api.dto.CreateVehicleFineRequest;
import com.car.rental.api.dto.CreateVehicleIssueRequest;
import com.car.rental.api.dto.CreateVehicleServiceRequest;
import com.car.rental.api.dto.OkResponse;
import com.car.rental.api.dto.UpdateOdometerRequest;
import com.car.rental.api.dto.UpdateVehicleIssueRequest;
import com.car.rental.api.dto.VehicleFineDto;
import com.car.rental.api.dto.VehicleHistoryDto;
import com.car.rental.api.dto.VehicleIssueDto;
import com.car.rental.api.dto.VehicleServiceDto;
import com.car.rental.service.VehicleHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

/**
 * HTTP API for vehicle history dossier: services, issues, fines, odometer.
 * Vehicle is identified by {@code plate} query param (Persian-safe, same pattern as VehicleController).
 */
@RestController
@RequestMapping("/api/vehicles/history")
public class VehicleHistoryController {

    private final VehicleHistoryService historyService;

    public VehicleHistoryController(VehicleHistoryService historyService) {
        this.historyService = historyService;
    }

    // ── Full history ───────────────────────────────────────────────────

    /**
     * Complete history dossier for a vehicle.
     * GET /api/vehicles/history?plate=11B22233
     */
    @GetMapping
    public VehicleHistoryDto getHistory(@RequestParam("plate") String plate) throws SQLException {
        requirePlate(plate);
        return historyService.getHistoryByPlate(plate.strip());
    }

    // ── Services ───────────────────────────────────────────────────────

    /**
     * List service records.
     * GET /api/vehicles/history/services?plate=...
     */
    @GetMapping("/services")
    public List<VehicleServiceDto> listServices(@RequestParam("plate") String plate) throws SQLException {
        requirePlate(plate);
        return historyService.listServices(plate.strip());
    }

    /**
     * Register a new service / maintenance event.
     * POST /api/vehicles/history/services?plate=...
     */
    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleServiceDto addService(@RequestParam("plate") String plate,
                                        @RequestBody CreateVehicleServiceRequest body)
            throws SQLException {
        requirePlate(plate);
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        return historyService.addService(
                plate.strip(),
                body.getServiceType(),
                body.getDescription(),
                body.getOdometer(),
                body.getCost(),
                body.getServiceDate(),
                body.getNextDueOdometer(),
                body.getNextDueDate(),
                body.getPerformedBy(),
                body.getNotes()
        );
    }

    // ── Issues ─────────────────────────────────────────────────────────

    /**
     * List issues / damage reports.
     * GET /api/vehicles/history/issues?plate=...
     */
    @GetMapping("/issues")
    public List<VehicleIssueDto> listIssues(@RequestParam("plate") String plate) throws SQLException {
        requirePlate(plate);
        return historyService.listIssues(plate.strip());
    }

    /**
     * Report a new issue.
     * POST /api/vehicles/history/issues?plate=...
     */
    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleIssueDto addIssue(@RequestParam("plate") String plate,
                                    @RequestBody CreateVehicleIssueRequest body)
            throws SQLException {
        requirePlate(plate);
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        return historyService.addIssue(
                plate.strip(),
                body.getReportedBy(),
                body.getTitle(),
                body.getDescription(),
                body.getSeverity(),
                body.getCost(),
                body.getReportedAt(),
                body.getNotes()
        );
    }

    /**
     * Update an existing issue (status, cost, notes, ...).
     * PATCH /api/vehicles/history/issues/{issueId}?plate=...
     */
    @PatchMapping("/issues/{issueId}")
    public VehicleIssueDto updateIssue(@RequestParam("plate") String plate,
                                       @PathVariable("issueId") int issueId,
                                       @RequestBody UpdateVehicleIssueRequest body)
            throws SQLException {
        requirePlate(plate);
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        return historyService.updateIssue(
                plate.strip(),
                issueId,
                body.getStatus(),
                body.getCost(),
                body.getResolvedAt(),
                body.getNotes(),
                body.getSeverity()
        );
    }

    // ── Fines ──────────────────────────────────────────────────────────

    /**
     * List fines.
     * GET /api/vehicles/history/fines?plate=...
     */
    @GetMapping("/fines")
    public List<VehicleFineDto> listFines(@RequestParam("plate") String plate) throws SQLException {
        requirePlate(plate);
        return historyService.listFines(plate.strip());
    }

    /**
     * Register a new fine.
     * POST /api/vehicles/history/fines?plate=...
     */
    @PostMapping("/fines")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleFineDto addFine(@RequestParam("plate") String plate,
                                  @RequestBody CreateVehicleFineRequest body)
            throws SQLException {
        requirePlate(plate);
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        return historyService.addFine(
                plate.strip(),
                body.getEmployeeId(),
                body.getFineType(),
                body.getAmount(),
                body.getFineDate(),
                body.getDueDate(),
                body.getDescription()
        );
    }

    /**
     * Mark a fine as paid.
     * PATCH /api/vehicles/history/fines/{fineId}/pay?plate=...
     * Optional body: { "paymentDate": "1404-01-15" } — if omitted, fine_date is used.
     */
    @PatchMapping("/fines/{fineId}/pay")
    public VehicleFineDto markFinePaid(@RequestParam("plate") String plate,
                                       @PathVariable("fineId") int fineId,
                                       @RequestBody(required = false) UpdateOdometerRequest ignored,
                                       @RequestParam(value = "paymentDate", required = false) String paymentDate)
            throws SQLException {
        requirePlate(plate);
        return historyService.markFinePaid(plate.strip(), fineId, paymentDate);
    }

    // ── Odometer ───────────────────────────────────────────────────────

    /**
     * Update current odometer reading.
     * PUT /api/vehicles/history/odometer?plate=...
     * Body: { "odometer": 45200 }
     */
    @PutMapping("/odometer")
    public OkResponse updateOdometer(@RequestParam("plate") String plate,
                                     @RequestBody UpdateOdometerRequest body)
            throws SQLException {
        requirePlate(plate);
        if (body == null || body.getOdometer() == null) {
            throw new IllegalArgumentException("فیلد odometer الزامی است");
        }
        historyService.updateOdometer(plate.strip(), body.getOdometer());
        return OkResponse.ok("کیلومتر به‌روزرسانی شد");
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private static void requirePlate(String plate) {
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("پارامتر plate الزامی است");
        }
    }
}
