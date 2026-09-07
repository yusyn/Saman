package com.car.rental.api;

import com.car.rental.api.dto.AddFingerRequest;
import com.car.rental.api.dto.EmployeeDto;
import com.car.rental.api.dto.OkResponse;
import com.car.rental.api.dto.RegisterEmployeeRequest;
import com.car.rental.model.Employee;
import com.car.rental.service.EmployeeService;
import com.car.rental.service.FingerprintException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeDto> list() throws SQLException {
        return employeeService.getAllEmployees().stream()
                .map(EmployeeDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{deviceUserId}")
    public EmployeeDto one(@PathVariable String deviceUserId) throws SQLException {
        Employee e = employeeService.findByDeviceUserId(deviceUserId);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "کارمند یافت نشد: " + deviceUserId);
        }
        return EmployeeDto.from(e);
    }

    /**
     * Official registration: blocks until fingerprint enroll finishes on the device,
     * then writes the employee to the database. May take 30–60+ seconds.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDto register(@RequestBody RegisterEmployeeRequest body)
            throws SQLException, FingerprintException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        Employee saved = employeeService.registerWithFingerprint(
                body.getDeviceUserId(),
                body.getName(),
                body.getPhone(),
                body.getFingerIndex());
        return EmployeeDto.from(saved);
    }

    /** Enroll an additional finger for an existing employee (device + DB already exist). */
    @PostMapping("/{deviceUserId}/fingers")
    public OkResponse addFinger(@PathVariable String deviceUserId,
                                @RequestBody AddFingerRequest body) throws FingerprintException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        employeeService.addFingerprint(deviceUserId, body.getFingerIndex());
        return OkResponse.ok("اثر انگشت اضافه شد");
    }

    @DeleteMapping("/{deviceUserId}")
    public OkResponse delete(@PathVariable String deviceUserId) throws SQLException {
        employeeService.deleteEmployee(deviceUserId);
        return OkResponse.ok("کارمند حذف شد");
    }
}
