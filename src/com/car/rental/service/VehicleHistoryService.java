package com.car.rental.service;

import com.car.rental.api.dto.RentalRecordDto;
import com.car.rental.api.dto.VehicleDto;
import com.car.rental.api.dto.VehicleFineDto;
import com.car.rental.api.dto.VehicleHistoryDto;
import com.car.rental.api.dto.VehicleIssueDto;
import com.car.rental.api.dto.VehicleServiceDto;
import com.car.rental.db.RentalRepository;
import com.car.rental.db.VehicleFineRepository;
import com.car.rental.db.VehicleIssueRepository;
import com.car.rental.db.VehicleRepository;
import com.car.rental.db.VehicleRepository.VehicleRow;
import com.car.rental.db.VehicleServiceRepository;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.VehicleFine;
import com.car.rental.model.VehicleIssue;
import com.car.rental.model.VehicleServiceRecord;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for vehicle history dossier:
 * services, issues, fines, usage (rentals), odometer and status badges.
 */
@Service
public class VehicleHistoryService {

    public static final String BADGE_DAMAGED = "DAMAGED";
    public static final String BADGE_IN_REPAIR = "IN_REPAIR";
    public static final String BADGE_NEEDS_SERVICE = "NEEDS_SERVICE";
    public static final String BADGE_HAS_FINE = "HAS_FINE";

    /** Default service interval in kilometers when next_due is not set. */
    private static final int DEFAULT_SERVICE_INTERVAL_KM = 5000;

    private final VehicleRepository vehicles;
    private final VehicleServiceRepository services;
    private final VehicleIssueRepository issues;
    private final VehicleFineRepository fines;
    private final RentalRepository rentals;

    public VehicleHistoryService(VehicleRepository vehicles,
                                 VehicleServiceRepository services,
                                 VehicleIssueRepository issues,
                                 VehicleFineRepository fines,
                                 RentalRepository rentals) {
        this.vehicles = vehicles;
        this.services = services;
        this.issues = issues;
        this.fines = fines;
        this.rentals = rentals;
    }

    // ── History aggregate ──────────────────────────────────────────────

    public VehicleHistoryDto getHistoryByPlate(String plate) throws SQLException {
        VehicleRow row = vehicles.findRowByPlate(plate);
        if (row == null) {
            throw new SQLException("وسیله فعالی با این پلاک یافت نشد: " + plate);
        }
        return buildHistory(row);
    }

    public VehicleHistoryDto getHistoryById(int vehicleId) throws SQLException {
        VehicleRow row = vehicles.findRowById(vehicleId);
        if (row == null) {
            throw new SQLException("وسیله یافت نشد: id=" + vehicleId);
        }
        return buildHistory(row);
    }

    private VehicleHistoryDto buildHistory(VehicleRow row) throws SQLException {
        int vehicleId = row.id;

        List<RentalRecord> rentalList = rentals.findByVehicleId(vehicleId);
        List<VehicleServiceRecord> serviceList = services.findByVehicleId(vehicleId);
        List<VehicleIssue> issueList = issues.findByVehicleId(vehicleId);
        List<VehicleFine> fineList = fines.findByVehicleId(vehicleId);

        double totalServiceCost = services.sumCostByVehicleId(vehicleId);
        double totalFineAmount = fines.sumAmountByVehicleId(vehicleId);
        double unpaidFineAmount = fines.sumUnpaidByVehicleId(vehicleId);

        List<String> badges = computeStatusBadges(row, serviceList, vehicleId);

        // Persist computed flags for quick list views later
        String flagsCsv = String.join(",", badges);
        if (row.statusFlags == null || !row.statusFlags.equals(flagsCsv)) {
            vehicles.updateStatusFlags(vehicleId, flagsCsv);
        }

        VehicleHistoryDto dto = new VehicleHistoryDto();
        dto.setVehicle(VehicleDto.from(row.toVehicle()));
        dto.setVehicleId(vehicleId);
        dto.setCurrentOdometer(row.currentOdometer);
        dto.setLastServiceOdometer(row.lastServiceOdometer);
        dto.setLastServiceDate(row.lastServiceDate);
        dto.setTotalRentals(rentalList.size());
        dto.setTotalServiceCost(totalServiceCost);
        dto.setTotalFineAmount(totalFineAmount);
        dto.setUnpaidFineAmount(unpaidFineAmount);
        dto.setStatusBadges(badges);
        dto.setRentals(rentalList.stream().map(RentalRecordDto::from).collect(Collectors.toList()));
        dto.setServices(serviceList.stream().map(VehicleServiceDto::from).collect(Collectors.toList()));
        dto.setIssues(issueList.stream().map(VehicleIssueDto::from).collect(Collectors.toList()));
        dto.setFines(fineList.stream().map(VehicleFineDto::from).collect(Collectors.toList()));
        return dto;
    }

    /**
     * Status badge rules:
     * - DAMAGED: open/in-progress issue with HIGH or CRITICAL severity
     * - IN_REPAIR: any issue currently IN_PROGRESS
     * - HAS_FINE: at least one unpaid fine
     * - NEEDS_SERVICE: current odometer >= next due (from latest service),
     *   or (no next_due set) last service + 5000 km exceeded
     */
    List<String> computeStatusBadges(VehicleRow row,
                                     List<VehicleServiceRecord> serviceList,
                                     int vehicleId) throws SQLException {
        List<String> badges = new ArrayList<>();

        if (issues.hasOpenHighSeverity(vehicleId)) {
            badges.add(BADGE_DAMAGED);
        }
        if (issues.hasInProgress(vehicleId)) {
            badges.add(BADGE_IN_REPAIR);
        }
        if (fines.hasUnpaid(vehicleId)) {
            badges.add(BADGE_HAS_FINE);
        }
        if (needsService(row, serviceList)) {
            badges.add(BADGE_NEEDS_SERVICE);
        }

        return badges;
    }

    private boolean needsService(VehicleRow row, List<VehicleServiceRecord> serviceList) {
        int current = row.currentOdometer;
        if (current <= 0) {
            return false;
        }

        // Prefer explicit next_due_odometer from the most recent service that has one
        for (VehicleServiceRecord s : serviceList) {
            if (s.getNextDueOdometer() != null && s.getNextDueOdometer() > 0) {
                return current >= s.getNextDueOdometer();
            }
        }

        // Fallback: last_service_odometer + default interval
        if (row.lastServiceOdometer != null && row.lastServiceOdometer > 0) {
            return current >= row.lastServiceOdometer + DEFAULT_SERVICE_INTERVAL_KM;
        }

        // No service history at all and odometer is high enough to suggest first service
        if (serviceList.isEmpty() && current >= DEFAULT_SERVICE_INTERVAL_KM) {
            return true;
        }

        return false;
    }

    // ── Service CRUD ───────────────────────────────────────────────────

    public VehicleServiceDto addService(String plate, String serviceType, String description,
                                        Integer odometer, Double cost, String serviceDate,
                                        Integer nextDueOdometer, String nextDueDate,
                                        String performedBy, String notes) throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        if (serviceDate == null || serviceDate.isBlank()) {
            throw new IllegalArgumentException("تاریخ سرویس الزامی است");
        }
        double c = cost != null ? cost : 0;
        if (c < 0) {
            throw new IllegalArgumentException("هزینه نمی‌تواند منفی باشد");
        }

        VehicleServiceRecord record = new VehicleServiceRecord(
                row.id,
                serviceType,
                description,
                odometer,
                c,
                serviceDate.strip(),
                nextDueOdometer,
                nextDueDate,
                performedBy,
                notes
        );
        int id = services.insert(record);

        // Keep vehicle summary fields in sync
        if (odometer != null && odometer > row.currentOdometer) {
            vehicles.updateOdometer(row.id, odometer);
        }
        vehicles.updateLastService(row.id, odometer != null ? odometer : row.currentOdometer, serviceDate.strip());

        // Refresh badges
        refreshStatusFlags(row.id);

        VehicleServiceRecord saved = services.findById(id);
        return VehicleServiceDto.from(saved);
    }

    public List<VehicleServiceDto> listServices(String plate) throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        return services.findByVehicleId(row.id).stream()
                .map(VehicleServiceDto::from)
                .collect(Collectors.toList());
    }

    // ── Issue CRUD ─────────────────────────────────────────────────────

    public VehicleIssueDto addIssue(String plate, Integer reportedBy, String title,
                                    String description, String severity, Double cost,
                                    String reportedAt, String notes) throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("عنوان خرابی الزامی است");
        }
        if (reportedAt == null || reportedAt.isBlank()) {
            throw new IllegalArgumentException("تاریخ گزارش الزامی است");
        }
        double c = cost != null ? cost : 0;

        VehicleIssue issue = new VehicleIssue(
                row.id,
                reportedBy,
                title.strip(),
                description,
                severity,
                VehicleIssue.STATUS_OPEN,
                c,
                reportedAt.strip(),
                notes
        );
        int id = issues.insert(issue);
        refreshStatusFlags(row.id);

        return VehicleIssueDto.from(issues.findById(id));
    }

    public VehicleIssueDto updateIssue(String plate, int issueId, String status, Double cost,
                                       String resolvedAt, String notes, String severity)
            throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        VehicleIssue existing = issues.findById(issueId);
        if (existing == null || existing.getVehicleId() != row.id) {
            throw new SQLException("خرابی یافت نشد");
        }

        if (status != null && !status.isBlank()) {
            existing.setStatus(status);
        }
        if (cost != null) {
            existing.setCost(cost);
        }
        if (resolvedAt != null) {
            existing.setResolvedAt(resolvedAt.isBlank() ? null : resolvedAt.strip());
        }
        if (notes != null) {
            existing.setNotes(notes);
        }
        if (severity != null && !severity.isBlank()) {
            existing.setSeverity(severity);
        }

        // Auto-set resolvedAt when moving to RESOLVED/CLOSED without explicit date
        if ((VehicleIssue.STATUS_RESOLVED.equals(existing.getStatus())
                || VehicleIssue.STATUS_CLOSED.equals(existing.getStatus()))
                && (existing.getResolvedAt() == null || existing.getResolvedAt().isBlank())) {
            // leave null — caller should supply; not inventing clock here
        }

        issues.update(existing);
        refreshStatusFlags(row.id);
        return VehicleIssueDto.from(issues.findById(issueId));
    }

    public List<VehicleIssueDto> listIssues(String plate) throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        return issues.findByVehicleId(row.id).stream()
                .map(VehicleIssueDto::from)
                .collect(Collectors.toList());
    }

    // ── Fine CRUD ──────────────────────────────────────────────────────

    public VehicleFineDto addFine(String plate, Integer employeeId, String fineType,
                                  Double amount, String fineDate, String dueDate,
                                  String description) throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("مبلغ خلافی باید بزرگ‌تر از صفر باشد");
        }
        if (fineDate == null || fineDate.isBlank()) {
            throw new IllegalArgumentException("تاریخ خلافی الزامی است");
        }

        VehicleFine fine = new VehicleFine(
                row.id,
                employeeId,
                fineType,
                amount,
                fineDate.strip(),
                dueDate,
                description
        );
        int id = fines.insert(fine);
        refreshStatusFlags(row.id);

        return VehicleFineDto.from(fines.findById(id));
    }

    public VehicleFineDto markFinePaid(String plate, int fineId, String paymentDate)
            throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        VehicleFine existing = fines.findById(fineId);
        if (existing == null || existing.getVehicleId() != row.id) {
            throw new SQLException("خلافی یافت نشد");
        }
        String payDate = (paymentDate != null && !paymentDate.isBlank())
                ? paymentDate.strip()
                : fineDateOrNow(existing.getFineDate());
        fines.markPaid(fineId, payDate);
        refreshStatusFlags(row.id);
        return VehicleFineDto.from(fines.findById(fineId));
    }

    public List<VehicleFineDto> listFines(String plate) throws SQLException {
        VehicleRow row = requireRowByPlate(plate);
        return fines.findByVehicleId(row.id).stream()
                .map(VehicleFineDto::from)
                .collect(Collectors.toList());
    }

    // ── Odometer ───────────────────────────────────────────────────────

    public void updateOdometer(String plate, int odometer) throws SQLException {
        if (odometer < 0) {
            throw new IllegalArgumentException("کیلومتر نمی‌تواند منفی باشد");
        }
        VehicleRow row = requireRowByPlate(plate);
        if (odometer < row.currentOdometer) {
            throw new IllegalArgumentException(
                    "کیلومتر جدید نمی‌تواند کمتر از مقدار فعلی (" + row.currentOdometer + ") باشد");
        }
        vehicles.updateOdometer(row.id, odometer);
        refreshStatusFlags(row.id);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private VehicleRow requireRowByPlate(String plate) throws SQLException {
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("پلاک الزامی است");
        }
        VehicleRow row = vehicles.findRowByPlate(plate.strip());
        if (row == null) {
            throw new SQLException("وسیله فعالی با این پلاک یافت نشد: " + plate);
        }
        return row;
    }

    private void refreshStatusFlags(int vehicleId) throws SQLException {
        VehicleRow row = vehicles.findRowById(vehicleId);
        if (row == null) {
            return;
        }
        List<VehicleServiceRecord> serviceList = services.findByVehicleId(vehicleId);
        List<String> badges = computeStatusBadges(row, serviceList, vehicleId);
        vehicles.updateStatusFlags(vehicleId, String.join(",", badges));
    }

    private static String fineDateOrNow(String fineDate) {
        return fineDate != null && !fineDate.isBlank() ? fineDate : "";
    }
}
