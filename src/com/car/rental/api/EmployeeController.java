package com.car.rental.api;

import com.car.rental.api.dto.CreateEmployeeRequest;
import com.car.rental.api.dto.EmployeeDto;
import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * DB-only create (no fingerprint device). Enough to test rentals over API.
     * Name should be English if you later sync the same id to ZK.
     */
    @PostMapping
    public EmployeeDto create(@RequestBody CreateEmployeeRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        if (body.getName() == null || body.getName().isBlank()) {
            throw new IllegalArgumentException("نام الزامی است");
        }
        String id = body.getDeviceUserId();
        if (id == null || id.isBlank()) {
            id = db.getNextDeviceUserId();
        } else if (db.isDeviceUserIdExists(id)) {
            throw new SQLException("شناسه کارمند قبلاً ثبت شده است: " + id);
        }
        String phone = body.getPhone() != null ? body.getPhone() : "";
        db.addEmployee(id, body.getName().trim(), phone);
        Employee saved = db.findByDeviceUserId(id);
        return EmployeeDto.from(saved);
    }
}
