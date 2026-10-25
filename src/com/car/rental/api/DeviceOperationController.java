package com.car.rental.api;

import com.car.rental.api.dto.DeviceOperationPage;
import com.car.rental.service.DeviceOperationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/device-operations")
public class DeviceOperationController {
    private final DeviceOperationService operations;
    public DeviceOperationController(DeviceOperationService operations){this.operations=operations;}
    @GetMapping("/pending")
    public DeviceOperationPage pending() throws SQLException{return operations.pending();}
}
