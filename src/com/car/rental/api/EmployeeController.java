package com.car.rental.api;

import com.car.rental.api.dto.EmployeeDto;
import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final DatabaseManager db;

    public EmployeeController(DatabaseManager db) {
        this.db = db;
    }

    @GetMapping
    public List<EmployeeDto> list() throws SQLException {
        return db.getAllEmployees().stream().map(EmployeeDto::from).collect(Collectors.toList());
    }

    @GetMapping("/{deviceUserId}")
    public EmployeeDto one(@PathVariable String deviceUserId) throws SQLException {
        Employee e = db.findByDeviceUserId(deviceUserId);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "کارمند یافت نشد: " + deviceUserId);
        }
        return EmployeeDto.from(e);
    }
}
