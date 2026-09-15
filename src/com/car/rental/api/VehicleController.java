package com.car.rental.api;

import com.car.rental.api.dto.CreateVehicleRequest;
import com.car.rental.api.dto.OkResponse;
import com.car.rental.api.dto.UpdateVehicleRequest;
import com.car.rental.api.dto.VehicleDto;
import com.car.rental.model.Vehicle;
import com.car.rental.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/available")
    public List<VehicleDto> available() throws SQLException {
        return vehicleService.getAvailableVehicles().stream()
                .map(VehicleDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<VehicleDto> all() throws SQLException {
        return vehicleService.getAllVehicles().stream()
                .map(VehicleDto::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto create(@RequestBody CreateVehicleRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        String type = Vehicle.normalizeType(body.getVehicleType());
        vehicleService.addVehicle(body.getName(), body.getPlate(), body.getColor(), type);
        return new VehicleDto(body.getName(), body.getPlate(), body.getColor(), "آزاد", type);
    }

    /**
     * Update name / plate / color / type. Identified by {@code oldPlate} in the body
     * (plate may contain Persian letters — safer than path variable).
     */
    @PutMapping
    public VehicleDto update(@RequestBody UpdateVehicleRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        String oldPlate = body.getOldPlate() != null ? body.getOldPlate().strip() : "";
        if (oldPlate.isEmpty()) {
            throw new IllegalArgumentException("پلاک قبلی (oldPlate) الزامی است");
        }

        Vehicle existing = findByPlate(oldPlate);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "وسیله یافت نشد: " + oldPlate);
        }
        if (isOnMission(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "وسیله در مأموریت است و قابل ویرایش نیست");
        }

        String type = body.getVehicleType() != null
                ? Vehicle.normalizeType(body.getVehicleType())
                : existing.getVehicleType();

        Vehicle updated = new Vehicle(
                body.getName(),
                body.getPlate(),
                body.getColor(),
                type,
                existing.getStatus()
        );
        vehicleService.updateVehicle(updated, oldPlate);
        return VehicleDto.from(updated);
    }

    /**
     * Soft-delete by plate query param (Persian-safe).
     */
    @DeleteMapping
    public OkResponse delete(@RequestParam("plate") String plate) throws SQLException {
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("پلاک الزامی است");
        }
        String p = plate.strip();
        Vehicle existing = findByPlate(p);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "وسیله یافت نشد: " + p);
        }
        if (isOnMission(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "وسیله در مأموریت است و قابل حذف نیست");
        }
        vehicleService.deleteVehicle(p);
        return OkResponse.ok("وسیله حذف شد");
    }

    private Vehicle findByPlate(String plate) throws SQLException {
        for (Vehicle v : vehicleService.getAllVehicles()) {
            if (plate.equals(v.getPlate())) {
                return v;
            }
        }
        return null;
    }

    private static boolean isOnMission(String status) {
        if (status == null) {
            return false;
        }
        return status.contains("مأموریت") || status.contains("ماموریت");
    }
}
