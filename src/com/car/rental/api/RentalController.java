package com.car.rental.api;

import com.car.rental.api.dto.ActiveRentalDto;
import com.car.rental.api.dto.OkResponse;
import com.car.rental.api.dto.PickupRequest;
import com.car.rental.api.dto.RentalRecordDto;
import com.car.rental.api.dto.ReturnRequest;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import com.car.rental.service.RentalService;
import com.car.rental.util.JalaliDate;
import com.car.rental.util.ReportExcelExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping("/pickup")
    public OkResponse pickup(@RequestBody PickupRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        String time = body.getPickupTime();
        if (time == null || time.isBlank()) {
            time = JalaliDate.formatDateTime(LocalDateTime.now());
        }
        rentalService.pickup(body.getDeviceUserId(), body.getPlate(), time, body.getDestination());
        return OkResponse.ok("تحویل ثبت شد");
    }

    @PostMapping("/return")
    public OkResponse returnCar(@RequestBody ReturnRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        String time = body.getReturnTime();
        if (time == null || time.isBlank()) {
            time = JalaliDate.formatDateTime(LocalDateTime.now());
        }
        boolean ok = rentalService.returnCar(body.getDeviceUserId(), time);
        if (!ok) {
            return OkResponse.fail("اجاره فعالی برای این کارمند یافت نشد");
        }
        return OkResponse.ok("برگشت ثبت شد");
    }

    @GetMapping("/active/{deviceUserId}")
    public RentalRecordDto active(@PathVariable String deviceUserId) throws SQLException {
        RentalRecord r = rentalService.getActiveRentalByDeviceUserId(deviceUserId);
        if (r == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "اجاره فعالی نیست");
        }
        return RentalRecordDto.from(r);
    }

    @GetMapping("/active")
    public List<ActiveRentalDto> activeRentals() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        return rentalService.getActiveRentals().stream().map(r -> {
            LocalDateTime pickup = parseJalaliDateTime(r.pickupDate);
            Long elapsedMinutes = pickup == null ? null : Math.max(0, Duration.between(pickup, now).toMinutes());
            String pickupDate = pickup == null ? r.pickupDate : JalaliDate.formatDateTimeMinutes(pickup);
            return new ActiveRentalDto(r.employeeName, r.carName, r.plate, r.destination,
                    pickupDate, r.vehicleType, elapsedMinutes);
        }).collect(Collectors.toList());
    }

    private static LocalDateTime parseJalaliDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.strip()
                    .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3')
                    .replace('۴', '4').replace('۵', '5').replace('۶', '6').replace('۷', '7')
                    .replace('۸', '8').replace('۹', '9')
                    .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
                    .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
                    .replace('٨', '8').replace('٩', '9');
            String[] parts = normalized.split("\\s+");
            String[] date = parts[0].split("[/.-]");
            if (date.length != 3) {
                return null;
            }
            int year = Integer.parseInt(date[0]);
            int month = Integer.parseInt(date[1]);
            int day = Integer.parseInt(date[2]);
            LocalTime time = LocalTime.MIDNIGHT;
            if (parts.length > 1) {
                String[] clock = parts[1].split(":");
                time = LocalTime.of(
                        Integer.parseInt(clock[0]),
                        clock.length > 1 ? Integer.parseInt(clock[1]) : 0,
                        clock.length > 2 ? Integer.parseInt(clock[2]) : 0
                );
            }
            LocalDate pickupDate = year >= 1700
                    ? LocalDate.of(year, month, day)
                    : new JalaliDate(year, month, day).toGregorian();
            return pickupDate.atTime(time);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @GetMapping("/report")
    public List<RentalRecordDto> report(
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String carName,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "ALL") String status
    ) throws SQLException {
        RentalReportFilter filter = buildFilter(employeeName, plate, carName, destination, dateFrom, dateTo, status);
        return rentalService.getRentalReport(filter).stream()
                .map(RentalRecordDto::from)
                .collect(Collectors.toList());
    }

    /**
     * CSV export (UTF-8 with BOM for Excel). Same filter query-params as /report.
     */
    @GetMapping(value = "/report/export.csv")
    public void exportCsv(
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String carName,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            HttpServletResponse response
    ) throws SQLException, IOException {
        RentalReportFilter filter = buildFilter(employeeName, plate, carName, destination, dateFrom, dateTo, status);
        List<RentalRecord> rows = rentalService.getRentalReport(filter);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"saman-report.csv\"");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            // BOM so Excel opens Persian correctly
            w.write('\ufeff');
            w.println("شناسه کارمند,نام کارمند,ماشین,رنگ,پلاک,تاریخ تحویل,تاریخ برگشت,مقصد");
            for (RentalRecord r : rows) {
                w.println(String.join(",",
                        csv(r.deviceUserId),
                        csv(r.employeeName),
                        csv(r.carName),
                        csv(r.carColor),
                        csv(r.plate),
                        csv(r.pickupDate),
                        csv(r.returnDate),
                        csv(r.destination)
                ));
            }
            w.flush();
        }
    }

    /**
     * Excel (.xlsx) export via Apache POI. Same filter query-params as /report.
     */
    @GetMapping(value = "/report/export.xlsx")
    public void exportXlsx(
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String carName,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            HttpServletResponse response
    ) throws SQLException, IOException {
        RentalReportFilter filter = buildFilter(employeeName, plate, carName, destination, dateFrom, dateTo, status);
        List<RentalRecord> rows = rentalService.getRentalReport(filter);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"saman-report.xlsx\"");

        ReportExcelExporter.export(rows, response.getOutputStream());
    }

    private static RentalReportFilter buildFilter(
            String employeeName,
            String plate,
            String carName,
            String destination,
            String dateFrom,
            String dateTo,
            String status
    ) {
        RentalReportFilter filter = new RentalReportFilter();
        filter.setEmployeeName(employeeName);
        filter.setPlate(plate);
        filter.setCarName(carName);
        filter.setDestination(destination);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        try {
            filter.setStatus(RentalReportFilter.Status.valueOf(
                    status == null ? "ALL" : status.trim().toUpperCase()));
        } catch (Exception e) {
            filter.setStatus(RentalReportFilter.Status.ALL);
        }
        return filter;
    }

    private static String csv(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
            return "\"" + t + "\"";
        }
        return t;
    }
}
