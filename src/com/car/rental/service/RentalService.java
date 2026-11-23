package com.car.rental.service;

import com.car.rental.db.EmployeeRepository;
import com.car.rental.db.RentalRepository;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Business logic for vehicle pickup / return and rental reports.
 */
@Service
public class RentalService {

    private final RentalRepository rentals;
    private final EmployeeRepository employees;
    private final SystemEventService systemEvents;

    public RentalService(RentalRepository rentals, EmployeeRepository employees, SystemEventService systemEvents) {
        this.rentals = rentals;
        this.employees = employees;
        this.systemEvents = systemEvents;
    }

    public void pickup(String deviceUserId, String vehiclePlate, String pickupTime, String destination)
            throws SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کاربر خالی است");
        }
        if (vehiclePlate == null || vehiclePlate.isBlank()) {
            throw new IllegalArgumentException("پلاک وسیله خالی است");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("مقصد الزامی است");
        }
        Employee employee = employees.findByDeviceUserId(deviceUserId);
        if (employee == null) {
            throw new IllegalArgumentException("کارمند با شناسه " + deviceUserId + " یافت نشد");
        }
        String licenseNum = employee.getDriverLicenseNumber();
        if (licenseNum == null || licenseNum.trim().isEmpty()) {
            throw new IllegalArgumentException("کارمند فاقد شماره گواهینامه ثبت‌شده است؛ امکان تحویل وسیله وجود ندارد.");
        }
        String licenseExp = employee.getDriverLicenseExpiry();
        if (licenseExp == null || licenseExp.trim().isEmpty()) {
            throw new IllegalArgumentException("تاریخ انقضای گواهینامه کارمند ثبت نشده است؛ امکان تحویل وسیله وجود ندارد.");
        }
        if (com.car.rental.util.JalaliDate.isExpired(licenseExp, java.time.LocalDate.now())) {
            throw new IllegalArgumentException("گواهینامه کارمند در تاریخ " + licenseExp + " منقضی شده است؛ امکان تحویل وسیله وجود ندارد.");
        }
        Map<String, ?> details=Map.of("employee",employee.getName(),"employeeId",deviceUserId,
                "plate",vehiclePlate,"destination",destination,"pickupTime",pickupTime==null?"":pickupTime);
        systemEvents.inTransaction(
                new SystemEventService.EventSpec("مأموریت‌ها","تحویل وسیله","وسیله برای مأموریت تحویل شد","مأموریت",deviceUserId,details),
                new SystemEventService.EventSpec("مأموریت‌ها","تحویل وسیله","تحویل وسیله انجام نشد","مأموریت",deviceUserId),
                conn->{rentals.insertRental(conn,deviceUserId,vehiclePlate,pickupTime,destination);return null;});
    }

    public boolean returnCar(String deviceUserId, String returnDate) throws SQLException {
        return returnCar(deviceUserId, returnDate, null, null);
    }

    public boolean returnCar(String deviceUserId, String returnDate, Integer returnOdometer,
                             java.util.List<com.car.rental.api.dto.CreateMissionReportRequest> reports) throws SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کاربر خالی است");
        }
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("employeeId", deviceUserId);
        details.put("returnTime", returnDate == null ? "" : returnDate);
        if (returnOdometer != null) details.put("returnOdometer", returnOdometer);
        if (reports != null) details.put("reportsCount", reports.size());

        return systemEvents.inTransaction(
                new SystemEventService.EventSpec("مأموریت‌ها","برگشت وسیله","وسیله از مأموریت برگشت داده شد","مأموریت",deviceUserId, details),
                new SystemEventService.EventSpec("مأموریت‌ها","برگشت وسیله","برگشت وسیله انجام نشد","مأموریت",deviceUserId),
                conn->rentals.returnCarByDeviceUserId(conn, deviceUserId, returnDate, returnOdometer, reports),
                Boolean.TRUE::equals);
    }

    public RentalRecord getActiveRentalByDeviceUserId(String deviceUserId) throws SQLException {
        return rentals.getActiveRentalByDeviceUserId(deviceUserId);
    }

    public List<RentalRecord> getRentalReport() throws SQLException {
        return rentals.getRentalReport();
    }

    public List<RentalRecord> getRentalReport(RentalReportFilter filter) throws SQLException {
        return rentals.getRentalReport(filter);
    }

    public List<RentalRecord> getActiveRentals() throws SQLException {
        RentalReportFilter filter = new RentalReportFilter();
        filter.setStatus(RentalReportFilter.Status.OPEN);
        return rentals.getRentalReport(filter);
    }

    public Employee findEmployeeByDeviceUserId(String deviceUserId) throws SQLException {
        return employees.findByDeviceUserId(deviceUserId);
    }
}
