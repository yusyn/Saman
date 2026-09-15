package com.car.rental.service;

import com.car.rental.db.VehicleRepository;
import com.car.rental.model.Vehicle;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for fleet / vehicles (cars and motorcycles).
 */
@Service
public class VehicleService {

    private final VehicleRepository vehicles;

    public VehicleService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    public void addVehicle(String name, String plate, String color, String vehicleType) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("نام وسیله الزامی است");
        }
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("پلاک الزامی است");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("رنگ الزامی است");
        }
        String type = Vehicle.normalizeType(vehicleType);
        String normalizedPlate = normalizeAndValidatePlate(plate, type);
        if (vehicles.isPlateTaken(normalizedPlate, null)) {
            throw new SQLException("این پلاک قبلاً ثبت شده است");
        }
        vehicles.addVehicle(name.strip(), normalizedPlate, color.strip(), type);
    }

    public void updateVehicle(Vehicle vehicle, String oldPlate) throws SQLException {
        if (vehicle == null) {
            throw new IllegalArgumentException("vehicle is null");
        }
        String model = vehicle.getModel();
        String plate = vehicle.getPlate();
        String color = vehicle.getColor();
        if (model == null || model.isBlank()
                || plate == null || plate.isBlank()
                || color == null || color.isBlank()) {
            throw new IllegalArgumentException("تمام فیلدهای وسیله باید پر شوند");
        }
        String type = Vehicle.normalizeType(vehicle.getVehicleType());
        String normalizedPlate = normalizeAndValidatePlate(plate, type);
        vehicle.setPlate(normalizedPlate);
        vehicle.setVehicleType(type);
        if (vehicles.isPlateTaken(normalizedPlate, oldPlate)) {
            throw new SQLException("این پلاک قبلاً برای وسیله دیگری ثبت شده است");
        }
        vehicles.updateVehicle(vehicle, oldPlate);
    }

    public void deleteVehicle(String plate) throws SQLException {
        vehicles.deleteVehicle(plate);
    }

    public List<Vehicle> getAvailableVehicles() throws SQLException {
        return vehicles.listAvailableVehicles();
    }

    public List<Vehicle> getAllVehicles() throws SQLException {
        return vehicles.listAllVehicles();
    }

    /**
     * Convert Persian/Arabic digits to Latin and validate by type.
     * Motorcycle: exactly 8 digits (3 city + 5 serial).
     * Car: non-empty after trim (format left to UI for now).
     */
    static String normalizeAndValidatePlate(String raw, String vehicleType) {
        String s = toLatinDigits(raw == null ? "" : raw).strip();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("پلاک الزامی است");
        }
        if (Vehicle.TYPE_MOTORCYCLE.equals(Vehicle.normalizeType(vehicleType))) {
            String digits = s.replaceAll("\\D", "");
            if (digits.length() != 8) {
                throw new IllegalArgumentException("پلاک موتور باید دقیقاً ۸ رقم باشد (۳ رقم شهر + ۵ رقم)");
            }
            return digits;
        }
        return s;
    }

    static String toLatinDigits(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= '\u06F0' && ch <= '\u06F9') { // Persian
                sb.append((char) ('0' + (ch - '\u06F0')));
            } else if (ch >= '\u0660' && ch <= '\u0669') { // Arabic-Indic
                sb.append((char) ('0' + (ch - '\u0660')));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
