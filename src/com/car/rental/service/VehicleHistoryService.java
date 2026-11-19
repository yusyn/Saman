package com.car.rental.service;

import com.car.rental.api.dto.RentalRecordDto;
import com.car.rental.api.dto.VehicleDocumentDto;
import com.car.rental.api.dto.VehicleDto;
import com.car.rental.api.dto.VehicleFineDto;
import com.car.rental.api.dto.VehicleHistoryDto;
import com.car.rental.api.dto.VehicleIssueDto;
import com.car.rental.api.dto.VehicleMissionReportDto;
import com.car.rental.api.dto.VehicleServiceDto;
import com.car.rental.db.EmployeeRepository;
import com.car.rental.db.RentalRepository;
import com.car.rental.db.VehicleDocumentRepository;
import com.car.rental.db.VehicleFineRepository;
import com.car.rental.db.VehicleIssueRepository;
import com.car.rental.db.VehicleMissionReportRepository;
import com.car.rental.db.VehicleRepository;
import com.car.rental.db.VehicleRepository.VehicleRow;
import com.car.rental.db.VehicleServiceRepository;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.VehicleDocument;
import com.car.rental.model.VehicleFine;
import com.car.rental.model.VehicleIssue;
import com.car.rental.model.VehicleMissionReport;
import com.car.rental.model.VehicleServiceRecord;
import com.car.rental.util.JalaliDate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    public static final String BADGE_HAS_FINE = "HAS_FINE";
    public static final String BADGE_INSURANCE_EXPIRING = "INSURANCE_EXPIRING";
    public static final String BADGE_INSURANCE_EXPIRED = "INSURANCE_EXPIRED";
    public static final String BADGE_INSPECTION_EXPIRING = "INSPECTION_EXPIRING";
    public static final String BADGE_INSPECTION_EXPIRED = "INSPECTION_EXPIRED";

    private final VehicleRepository vehicles;
    private final VehicleServiceRepository services;
    private final VehicleIssueRepository issues;
    private final VehicleFineRepository fines;
    private final RentalRepository rentals;
    private final VehicleMissionReportRepository missionReports;
    private final VehicleDocumentRepository vehicleDocuments;
    private final EmployeeRepository employees;

    public VehicleHistoryService(VehicleRepository vehicles,
                                 VehicleServiceRepository services,
                                 VehicleIssueRepository issues,
                                 VehicleFineRepository fines,
                                 RentalRepository rentals,
                                 VehicleMissionReportRepository missionReports,
                                 VehicleDocumentRepository vehicleDocuments,
                                 EmployeeRepository employees) {
        this.vehicles = vehicles;
        this.services = services;
        this.issues = issues;
        this.fines = fines;
        this.rentals = rentals;
        this.missionReports = missionReports;
        this.vehicleDocuments = vehicleDocuments;
        this.employees = employees;
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
        List<VehicleMissionReport> reportList = missionReports.findByVehicleId(vehicleId);
        List<VehicleDocument> docList = vehicleDocuments.findByVehicleId(vehicleId);

        double totalServiceCost = services.sumCostByVehicleId(vehicleId)
                + missionReports.sumAmountByVehicleIdAndType(vehicleId, "SERVICE");
        double totalFineAmount = fines.sumAmountByVehicleId(vehicleId)
                + missionReports.sumAmountByVehicleIdAndType(vehicleId, "FINE");
        double unpaidFineAmount = fines.sumUnpaidByVehicleId(vehicleId)
                + missionReports.sumUnpaidFinesByVehicleId(vehicleId);
        double totalFuelCost = missionReports.sumAmountByVehicleIdAndType(vehicleId, "FUEL");
        double totalFuelLiters = reportList.stream()
                .filter(r -> "FUEL".equalsIgnoreCase(r.getReportType()))
                .mapToDouble(VehicleMissionReport::getLiters)
                .sum();

        List<String> badges = computeStatusBadges(row, serviceList, vehicleId);

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
        dto.setTotalFuelCost(totalFuelCost);
        dto.setTotalFuelLiters(totalFuelLiters);
        dto.setStatusBadges(badges);
        dto.setRentals(rentalList.stream().map(RentalRecordDto::from).collect(Collectors.toList()));
        dto.setServices(serviceList.stream().map(VehicleServiceDto::from).collect(Collectors.toList()));
        dto.setIssues(issueList.stream().map(VehicleIssueDto::from).collect(Collectors.toList()));
        dto.setFines(fineList.stream().map(VehicleFineDto::from).collect(Collectors.toList()));

        List<VehicleMissionReportDto> reportDtos = new ArrayList<>();
        for (VehicleMissionReport r : reportList) {
            VehicleMissionReportDto rDto = VehicleMissionReportDto.from(r);
            if (r.getEmployeeId() != null && r.getEmployeeId() > 0) {
                try {
                    Employee emp = employees.findById(r.getEmployeeId());
                    if (emp != null) rDto.setEmployeeName(emp.getName());
                } catch (Exception ignored) {}
            }
            reportDtos.add(rDto);
        }
        dto.setMissionReports(reportDtos);
        dto.setDocuments(docList.stream().map(VehicleDocumentDto::from).collect(Collectors.toList()));
        return dto;
    }

    /**
     * Status badge rules:
     * - DAMAGED: open/in-progress issue with HIGH or CRITICAL severity, or open accident report
     * - IN_REPAIR: any issue currently IN_PROGRESS
     * - HAS_FINE: at least one unpaid fine (legacy or mission fine)
     * - INSURANCE_EXPIRED / INSURANCE_EXPIRING: policy expired or expiring within 30 days
     * - INSPECTION_EXPIRED / INSPECTION_EXPIRING: inspection expired or expiring within 30 days
     */
    List<String> computeStatusBadges(VehicleRow row,
                                     List<VehicleServiceRecord> serviceList,
                                     int vehicleId) throws SQLException {
        List<String> badges = new ArrayList<>();

        if (issues.hasOpenHighSeverity(vehicleId) || missionReports.hasOpenAccident(vehicleId)) {
            badges.add(BADGE_DAMAGED);
        }
        if (issues.hasInProgress(vehicleId)) {
            badges.add(BADGE_IN_REPAIR);
        }
        if (fines.hasUnpaid(vehicleId) || missionReports.hasUnpaidFine(vehicleId)) {
            badges.add(BADGE_HAS_FINE);
        }

        List<VehicleDocument> docs = vehicleDocuments.findByVehicleId(vehicleId);
        boolean insExp = false, insSoon = false, inspExp = false, inspSoon = false;
        for (VehicleDocument doc : docs) {
            Long days = daysUntilDate(doc.getExpiryDate());
            if (days != null) {
                String type = doc.getDocumentType();
                if ("THIRD_PARTY_INSURANCE".equalsIgnoreCase(type) || "BODY_INSURANCE".equalsIgnoreCase(type)) {
                    if (days < 0) insExp = true;
                    else if (days <= 30) insSoon = true;
                } else if ("TECHNICAL_INSPECTION".equalsIgnoreCase(type)) {
                    if (days < 0) inspExp = true;
                    else if (days <= 30) inspSoon = true;
                }
            }
        }
        if (insExp) badges.add(BADGE_INSURANCE_EXPIRED);
        else if (insSoon) badges.add(BADGE_INSURANCE_EXPIRING);

        if (inspExp) badges.add(BADGE_INSPECTION_EXPIRED);
        else if (inspSoon) badges.add(BADGE_INSPECTION_EXPIRING);

        return badges;
    }

    static Long daysUntilDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            String clean = dateStr.trim().replace('/', '-');
            String[] parts = clean.split("-");
            if (parts.length != 3) return null;
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            if (y < 1700) {
                JalaliDate jd = new JalaliDate(y, m, d);
                LocalDate greg = jd.toGregorian();
                return ChronoUnit.DAYS.between(LocalDate.now(), greg);
            } else {
                LocalDate greg = LocalDate.of(y, m, d);
                return ChronoUnit.DAYS.between(LocalDate.now(), greg);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public VehicleMissionReportDto addRetroactiveReport(int rentalId, com.car.rental.api.dto.CreateMissionReportRequest body) throws SQLException {
        if (body == null || body.getReportType() == null || body.getReportType().isBlank()) {
            throw new IllegalArgumentException("نوع گزارش الزامی است");
        }
        RentalRecord rental = rentals.findById(rentalId);
        if (rental == null) {
            throw new SQLException("مأموریت یافت نشد: id=" + rentalId);
        }
        VehicleRow row = vehicles.findRowByPlate(rental.plate);
        if (row == null) {
            throw new SQLException("وسیله یافت نشد: " + rental.plate);
        }
        Employee emp = employees.findByDeviceUserId(rental.deviceUserId);
        Integer empId = emp != null ? emp.getId() : null;

        String rDate = body.getReportDate() != null && !body.getReportDate().isBlank()
                ? body.getReportDate().strip()
                : (rental.returnDate != null && !rental.returnDate.contains("منتظر") ? rental.returnDate : rental.pickupDate);

        VehicleMissionReport report = new VehicleMissionReport(
                0,
                rentalId,
                row.id,
                empId,
                body.getReportType().toUpperCase().trim(),
                body.getTitle() != null && !body.getTitle().isBlank() ? body.getTitle().strip() : body.getReportType(),
                body.getDescription(),
                body.getAmount() != null ? body.getAmount() : 0.0,
                body.getLiters() != null ? body.getLiters() : 0.0,
                body.getSeverity(),
                body.getFineType(),
                false,
                body.getAttachmentId(),
                rDate,
                null
        );
        int id = missionReports.insert(report);
        refreshStatusFlags(row.id);

        VehicleMissionReportDto dto = VehicleMissionReportDto.from(report);
        dto.setId(id);
        if (emp != null) dto.setEmployeeName(emp.getName());
        return dto;
    }

    public void payMissionFine(int reportId) throws SQLException {
        missionReports.markFinePaid(reportId);
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
