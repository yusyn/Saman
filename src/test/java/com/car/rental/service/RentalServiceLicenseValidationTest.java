package com.car.rental.service;

import com.car.rental.db.EmployeeRepository;
import com.car.rental.db.RentalRepository;
import com.car.rental.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RentalServiceLicenseValidationTest {

    private RentalRepository rentals;
    private EmployeeRepository employees;
    private SystemEventService systemEvents;
    private RentalService rentalService;

    @BeforeEach
    void setUp() {
        rentals = Mockito.mock(RentalRepository.class);
        employees = Mockito.mock(EmployeeRepository.class);
        systemEvents = Mockito.mock(SystemEventService.class);
        rentalService = new RentalService(rentals, employees, systemEvents);
    }

    @Test
    @DisplayName("Pickup throws IllegalArgumentException when employee has no driver license number")
    void testPickupWithoutLicenseThrows() throws SQLException {
        Employee emp = new Employee(1, "1001", "Ali", "09120000000", true, false);
        emp.setDriverLicenseNumber(null);
        emp.setDriverLicenseExpiry("1406/01/01");
        when(employees.findByDeviceUserId("1001")).thenReturn(emp);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                rentalService.pickup("1001", "12B345", "1405/01/01", "Tehran"));
        assertTrue(ex.getMessage().contains("شماره گواهینامه"));
    }

    @Test
    @DisplayName("Pickup throws IllegalArgumentException when driver license is expired")
    void testPickupWithExpiredLicenseThrows() throws SQLException {
        Employee emp = new Employee(1, "1001", "Ali", "09120000000", true, false);
        emp.setDriverLicenseNumber("LIC-1234");
        emp.setDriverLicenseExpiry("1400/01/01");
        when(employees.findByDeviceUserId("1001")).thenReturn(emp);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                rentalService.pickup("1001", "12B345", "1405/01/01", "Tehran"));
        assertTrue(ex.getMessage().contains("منقضی"));
    }

    @Test
    @DisplayName("Pickup succeeds when employee has valid, unexpired driver license")
    void testPickupWithValidLicenseSucceeds() throws SQLException {
        Employee emp = new Employee(1, "1001", "Ali", "09120000000", true, false);
        emp.setDriverLicenseNumber("LIC-1234");
        emp.setDriverLicenseExpiry("1420/01/01");
        when(employees.findByDeviceUserId("1001")).thenReturn(emp);

        assertDoesNotThrow(() ->
                rentalService.pickup("1001", "12B345", "1405/01/01", "Tehran"));
    }
}
