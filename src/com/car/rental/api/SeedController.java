package com.car.rental.api;

import com.car.rental.api.dto.OkResponse;
import com.car.rental.db.EmployeeRepository;
import com.car.rental.model.Vehicle;
import com.car.rental.service.VehicleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

/**
 * One-shot demo data for an empty database (dev / first server setup).
 * Safe to call multiple times: skips rows that already exist.
 */
@RestController
@RequestMapping("/api/seed")
public class SeedController {

    private final EmployeeRepository employees;
    private final VehicleService vehicleService;

    public SeedController(EmployeeRepository employees, VehicleService vehicleService) {
        this.employees = employees;
        this.vehicleService = vehicleService;
    }

    @PostMapping("/demo")
    public OkResponse demo() throws SQLException {
        int employeeCount = 0;
        int vehicles = 0;

        employeeCount += addEmployeeIfAbsent("1001", "Ali Rezaei", "09120000001");
        employeeCount += addEmployeeIfAbsent("1002", "Sara Mohammadi", "09120000002");
        employeeCount += addEmployeeIfAbsent("1003", "Reza Karimi", "09120000003");

        vehicles += addVehicleIfAbsent("Pride", "11ل222ایران33", "White", Vehicle.TYPE_CAR);
        vehicles += addVehicleIfAbsent("Samand", "22ب333ایران44", "Black", Vehicle.TYPE_CAR);
        vehicles += addVehicleIfAbsent("Dena", "33د444ایران55", "Silver", Vehicle.TYPE_CAR);
        vehicles += addVehicleIfAbsent("Honda 125", "12356789", "Black", Vehicle.TYPE_MOTORCYCLE);
        vehicles += addVehicleIfAbsent("Yamaha", "45612345", "Red", Vehicle.TYPE_MOTORCYCLE);

        return OkResponse.ok("Seed done. newEmployees=" + employeeCount + " newVehicles=" + vehicles);
    }

    private int addEmployeeIfAbsent(String id, String name, String phone) throws SQLException {
        if (employees.isDeviceUserIdExists(id)) {
            return 0;
        }
        employees.addEmployee(id, name, phone);
        return 1;
    }

    private int addVehicleIfAbsent(String name, String plate, String color, String type) throws SQLException {
        try {
            vehicleService.addVehicle(name, plate, color, type);
            return 1;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("قبلاً")) {
                return 0;
            }
            throw e;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}
