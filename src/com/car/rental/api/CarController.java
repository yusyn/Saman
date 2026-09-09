package com.car.rental.api;

import com.car.rental.api.dto.CarDto;
import com.car.rental.api.dto.CreateCarRequest;
import com.car.rental.api.dto.OkResponse;
import com.car.rental.api.dto.UpdateCarRequest;
import com.car.rental.model.Car;
import com.car.rental.service.CarService;
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
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping("/available")
    public List<CarDto> available() throws SQLException {
        List<Car> cars = carService.getAvailableCars();
        return cars.stream().map(CarDto::from).collect(Collectors.toList());
    }

    @GetMapping
    public List<CarDto> all() throws SQLException {
        return carService.getAllCars().stream().map(CarDto::from).collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarDto create(@RequestBody CreateCarRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        carService.addCar(body.getName(), body.getPlate(), body.getColor());
        return new CarDto(body.getName(), body.getPlate(), body.getColor(), "آزاد");
    }

    /**
     * Update name / plate / color. Identified by {@code oldPlate} in the body
     * (plate may contain Persian letters — safer than path variable).
     */
    @PutMapping
    public CarDto update(@RequestBody UpdateCarRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        String oldPlate = body.getOldPlate() != null ? body.getOldPlate().strip() : "";
        if (oldPlate.isEmpty()) {
            throw new IllegalArgumentException("پلاک قبلی (oldPlate) الزامی است");
        }

        Car existing = findByPlate(oldPlate);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ماشین یافت نشد: " + oldPlate);
        }
        if (isOnMission(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ماشین در مأموریت است و قابل ویرایش نیست");
        }

        Car updated = new Car(
                body.getName(),
                body.getPlate(),
                body.getColor(),
                existing.getStatus()
        );
        carService.updateCar(updated, oldPlate);
        return CarDto.from(updated);
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
        Car existing = findByPlate(p);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ماشین یافت نشد: " + p);
        }
        if (isOnMission(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ماشین در مأموریت است و قابل حذف نیست");
        }
        carService.deleteCar(p);
        return OkResponse.ok("ماشین حذف شد");
    }

    private Car findByPlate(String plate) throws SQLException {
        for (Car c : carService.getAllCars()) {
            if (plate.equals(c.getPlate())) {
                return c;
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
