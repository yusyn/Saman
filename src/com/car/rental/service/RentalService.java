package com.car.rental.service;

import com.car.rental.db.EmployeeRepository;
import com.car.rental.db.RentalRepository;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for vehicle pickup / return and rental reports.
 */
@Service
public class RentalService {

    private final RentalRepository rentals;
    private final EmployeeRepository employees;

    public RentalService(RentalRepository rentals, EmployeeRepository employees) {
        this.rentals = rentals;
        this.employees = employees;
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
        rentals.insertRental(deviceUserId, vehiclePlate, pickupTime, destination);
    }

    public boolean returnCar(String deviceUserId, String returnDate) throws SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کاربر خالی است");
        }
        return rentals.returnCarByDeviceUserId(deviceUserId, returnDate);
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

    public Employee findEmployeeByDeviceUserId(String deviceUserId) throws SQLException {
        return employees.findByDeviceUserId(deviceUserId);
    }
}
