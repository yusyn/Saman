package com.car.rental.api;

import com.car.rental.api.dto.CarDto;
import com.car.rental.model.Car;
import com.car.rental.service.CarService;
import org.springframework.web.bind.annotation.GetMapping;
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
}
