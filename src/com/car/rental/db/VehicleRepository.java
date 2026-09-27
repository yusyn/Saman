package com.car.rental.db;

import com.car.rental.model.Vehicle;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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

    /**
     * Returns vehicle id or -1 if not found (does not throw).
     */
    public int findIdByPlate(String plate) throws SQLException {
        String sql = "SELECT id FROM VehicleTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
                return -1;
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
        String sql = "SELECT name, color, plate, vehicle_type, is_rented, status_flags FROM VehicleTable " +
                "WHERE is_deleted = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean rented = rs.getInt("is_rented") == 1;
                Vehicle v = new Vehicle(
                        rs.getString("name"),
                        rs.getString("plate"),
                        rs.getString("color"),
                        rs.getString("vehicle_type"),
                        rented ? "در مأموریت" : "آزاد"
                );
                v.setStatusFlags(rs.getString("status_flags"));
                list.add(v);
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

    /** Load basic vehicle row by id (including history fields). */
    public VehicleRow findRowById(int id) throws SQLException {
        String sql = "SELECT id, name, plate, color, vehicle_type, is_rented, " +
                "current_odometer, last_service_odometer, last_service_date, status_flags " +
                "FROM VehicleTable WHERE id = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    public VehicleRow findRowByPlate(String plate) throws SQLException {
        String sql = "SELECT id, name, plate, color, vehicle_type, is_rented, " +
                "current_odometer, last_service_odometer, last_service_date, status_flags " +
                "FROM VehicleTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    public void updateOdometer(int vehicleId, int odometer) throws SQLException {
        String sql = "UPDATE VehicleTable SET current_odometer = ? WHERE id = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, odometer);
            stmt.setInt(2, vehicleId);
            int n = stmt.executeUpdate();
            if (n == 0) {
                throw new SQLException("وسیله یافت نشد");
            }
        }
    }

    public void updateLastService(int vehicleId, Integer odometer, String serviceDate) throws SQLException {
        String sql = "UPDATE VehicleTable SET last_service_odometer = ?, last_service_date = ? " +
                "WHERE id = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (odometer == null) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, odometer);
            }
            stmt.setString(2, serviceDate);
            stmt.setInt(3, vehicleId);
            int n = stmt.executeUpdate();
            if (n == 0) {
                throw new SQLException("وسیله یافت نشد");
            }
        }
    }

    public void updateStatusFlags(int vehicleId, String flags) throws SQLException {
        String sql = "UPDATE VehicleTable SET status_flags = ? WHERE id = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, flags != null ? flags : "");
            stmt.setInt(2, vehicleId);
            stmt.executeUpdate();
        }
    }

    private static VehicleRow mapRow(ResultSet rs) throws SQLException {
        VehicleRow row = new VehicleRow();
        row.id = rs.getInt("id");
        row.name = rs.getString("name");
        row.plate = rs.getString("plate");
        row.color = rs.getString("color");
        row.vehicleType = rs.getString("vehicle_type");
        row.rented = rs.getInt("is_rented") == 1;
        row.currentOdometer = rs.getInt("current_odometer");
        Object lastOdo = rs.getObject("last_service_odometer");
        row.lastServiceOdometer = lastOdo != null ? (Integer) lastOdo : null;
        row.lastServiceDate = rs.getString("last_service_date");
        row.statusFlags = rs.getString("status_flags");
        return row;
    }

    /** Lightweight row with history fields (not the domain Vehicle model). */
    public static class VehicleRow {
        public int id;
        public String name;
        public String plate;
        public String color;
        public String vehicleType;
        public boolean rented;
        public int currentOdometer;
        public Integer lastServiceOdometer;
        public String lastServiceDate;
        public String statusFlags;

        public Vehicle toVehicle() {
            return new Vehicle(
                    name,
                    plate,
                    color,
                    vehicleType,
                    rented ? "در مأموریت" : "آزاد"
            );
        }
    }
}
