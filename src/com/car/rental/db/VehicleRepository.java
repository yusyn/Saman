package com.car.rental.db;

import com.car.rental.model.Vehicle;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for fleet / vehicles (VehicleTable).
 */
@Repository
public class VehicleRepository {

    private final DataSource dataSource;

    public VehicleRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int getVehicleIdByPlate(String plate) throws SQLException {
        String sql = "SELECT id FROM VehicleTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("وسیله فعالی با این پلاک یافت نشد: " + plate);
                }
            }
        }
    }

    public boolean isPlateTaken(String plate, String excludePlate) throws SQLException {
        if (plate == null || plate.isBlank()) {
            return false;
        }
        if (excludePlate != null && !excludePlate.isBlank() && excludePlate.equals(plate)) {
            return false;
        }
        String sql = "SELECT 1 FROM VehicleTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return excludePlate == null || !excludePlate.equals(plate);
            }
        }
    }

    public List<Vehicle> listAvailableVehicles() throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT name, color, plate, vehicle_type FROM VehicleTable " +
                "WHERE is_deleted = 0 AND is_rented = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Vehicle(
                        rs.getString("name"),
                        rs.getString("plate"),
                        rs.getString("color"),
                        rs.getString("vehicle_type"),
                        "آزاد"
                ));
            }
        }
        return list;
    }

    public List<Vehicle> listAllVehicles() throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT name, color, plate, vehicle_type, is_rented FROM VehicleTable " +
                "WHERE is_deleted = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean rented = rs.getInt("is_rented") == 1;
                list.add(new Vehicle(
                        rs.getString("name"),
                        rs.getString("plate"),
                        rs.getString("color"),
                        rs.getString("vehicle_type"),
                        rented ? "در مأموریت" : "آزاد"
                ));
            }
        }
        return list;
    }

    public void addVehicle(String name, String plate, String color, String vehicleType) throws SQLException {
        String sql = "INSERT INTO VehicleTable(name, plate, color, vehicle_type) VALUES(?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, plate);
            stmt.setString(3, color);
            stmt.setString(4, Vehicle.normalizeType(vehicleType));
            stmt.executeUpdate();
        }
    }

    public void updateVehicle(Vehicle vehicle, String oldPlate) throws SQLException {
        String sql = "UPDATE VehicleTable SET name = ?, color = ?, plate = ?, vehicle_type = ? " +
                "WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicle.getModel());
            stmt.setString(2, vehicle.getColor());
            stmt.setString(3, vehicle.getPlate());
            stmt.setString(4, Vehicle.normalizeType(vehicle.getVehicleType()));
            stmt.setString(5, oldPlate);
            int n = stmt.executeUpdate();
            if (n == 0) {
                throw new SQLException("وسیله فعالی برای به‌روزرسانی یافت نشد");
            }
        }
    }

    public void deleteVehicle(String plate) throws SQLException {
        String checkSql = "SELECT is_rented FROM VehicleTable WHERE plate = ? AND is_deleted = 0";
        String sql = "UPDATE VehicleTable SET is_deleted = 1 WHERE plate = ? AND is_deleted = 0 AND is_rented = 0";
        try (Connection conn = getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, plate);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("وسیله فعالی یافت نشد");
                    }
                    if (rs.getInt("is_rented") == 1) {
                        throw new SQLException("وسیله در مأموریت است و قابل حذف نیست");
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, plate);
                int n = stmt.executeUpdate();
                if (n == 0) {
                    throw new SQLException("حذف وسیله انجام نشد");
                }
            }
        }
    }
}
