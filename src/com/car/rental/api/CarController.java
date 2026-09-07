package com.car.rental.api;

import com.car.rental.api.dto.CarDto;
import com.car.rental.api.dto.CreateCarRequest;
import com.car.rental.api.dto.OkResponse;
import com.car.rental.model.Car;
import com.car.rental.service.CarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public OkResponse create(@RequestBody CreateCarRequest body) throws SQLException {
        if (body == null) {
            throw new IllegalArgumentException("بدنه درخواست خالی است");
        }
        carService.addCar(body.getName(), body.getPlate(), body.getColor());
        return OkResponse.ok("ماشین ثبت شد: " + body.getPlate());
    }
}
