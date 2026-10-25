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
        Map<String, ?> details=Map.of("employee",employee==null?deviceUserId:employee.getName(),"employeeId",deviceUserId,
                "plate",vehiclePlate,"destination",destination,"pickupTime",pickupTime==null?"":pickupTime);
        systemEvents.inTransaction(
                new SystemEventService.EventSpec("مأموریت‌ها","تحویل وسیله","وسیله برای مأموریت تحویل شد","مأموریت",deviceUserId,details),
                new SystemEventService.EventSpec("مأموریت‌ها","تحویل وسیله","تحویل وسیله انجام نشد","مأموریت",deviceUserId),
                conn->{rentals.insertRental(conn,deviceUserId,vehiclePlate,pickupTime,destination);return null;});
    }

    public boolean returnCar(String deviceUserId, String returnDate) throws SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کاربر خالی است");
        }
        return systemEvents.inTransaction(
                new SystemEventService.EventSpec("مأموریت‌ها","برگشت وسیله","وسیله از مأموریت برگشت داده شد","مأموریت",deviceUserId,
                        Map.of("employeeId",deviceUserId,"returnTime",returnDate==null?"":returnDate)),
                new SystemEventService.EventSpec("مأموریت‌ها","برگشت وسیله","برگشت وسیله انجام نشد","مأموریت",deviceUserId),
                conn->rentals.returnCarByDeviceUserId(conn,deviceUserId,returnDate),
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
