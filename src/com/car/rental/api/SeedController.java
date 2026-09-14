package com.car.rental.api;

import com.car.rental.api.dto.OkResponse;
import com.car.rental.db.DatabaseManager;
import com.car.rental.service.CarService;
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

    private final DatabaseManager db;
    private final CarService carService;

    public SeedController(DatabaseManager db, CarService carService) {
        this.db = db;
        this.carService = carService;
    }

    @PostMapping("/demo")
    public OkResponse demo() throws SQLException {
        int employees = 0;
        int cars = 0;

        employees += addEmployeeIfAbsent("1001", "Ali Rezaei", "09120000001");
        employees += addEmployeeIfAbsent("1002", "Sara Mohammadi", "09120000002");
        employees += addEmployeeIfAbsent("1003", "Reza Karimi", "09120000003");

        cars += addCarIfAbsent("Pride", "11B22233", "White");
        cars += addCarIfAbsent("Samand", "22C33344", "Black");
        cars += addCarIfAbsent("Dena", "33D44455", "Silver");

        return OkResponse.ok("Seed done. newEmployees=" + employees + " newCars=" + cars);
    }

    private int addEmployeeIfAbsent(String id, String name, String phone) throws SQLException {
        if (db.isDeviceUserIdExists(id)) {
            return 0;
        }
        db.addEmployee(id, name, phone);
        return 1;
    }

    private int addCarIfAbsent(String name, String plate, String color) throws SQLException {
        if (db.isPlateTaken(plate, null)) {
            return 0;
        }
        carService.addCar(name, plate, color);
        return 1;
    }
}
