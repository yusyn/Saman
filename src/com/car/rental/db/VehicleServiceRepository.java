package com.car.rental.db;

import com.car.rental.model.VehicleServiceRecord;
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
 * Persistence for vehicle service / maintenance history (VehicleService).
 */
@Repository
public class VehicleServiceRepository {

    private final DataSource dataSource;

    public VehicleServiceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int insert(VehicleServiceRecord record) throws SQLException {
        String sql = "INSERT INTO VehicleService(" +
                "vehicle_id, service_type, description, odometer, cost, service_date, " +
                "next_due_odometer, next_due_date, performed_by, notes) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, record.getVehicleId());
            stmt.setString(2, record.getServiceType());
            stmt.setString(3, record.getDescription());
            setNullableInt(stmt, 4, record.getOdometer());
            stmt.setDouble(5, record.getCost());
            stmt.setString(6, record.getServiceDate());
            setNullableInt(stmt, 7, record.getNextDueOdometer());
            stmt.setString(8, record.getNextDueDate());
            stmt.setString(9, record.getPerformedBy());
            stmt.setString(10, record.getNotes());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        }
    }

    public List<VehicleServiceRecord> findByVehicleId(int vehicleId) throws SQLException {
        List<VehicleServiceRecord> list = new ArrayList<>();
        String sql = "SELECT id, vehicle_id, service_type, description, odometer, cost, " +
                "service_date, next_due_odometer, next_due_date, performed_by, notes, created_at " +
                "FROM VehicleService WHERE vehicle_id = ? ORDER BY service_date DESC, id DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public VehicleServiceRecord findById(int id) throws SQLException {
        String sql = "SELECT id, vehicle_id, service_type, description, odometer, cost, " +
                "service_date, next_due_odometer, next_due_date, performed_by, notes, created_at " +
                "FROM VehicleService WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
                return null;
            }
        }
    }

    public double sumCostByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(cost), 0) AS total FROM VehicleService WHERE vehicle_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
                return 0;
            }
        }
    }

    private static VehicleServiceRecord map(ResultSet rs) throws SQLException {
        Integer odometer = (Integer) rs.getObject("odometer");
        Integer nextDueOdometer = (Integer) rs.getObject("next_due_odometer");
        return new VehicleServiceRecord(
                rs.getInt("id"),
                rs.getInt("vehicle_id"),
                rs.getString("service_type"),
                rs.getString("description"),
                odometer,
                rs.getDouble("cost"),
                rs.getString("service_date"),
                nextDueOdometer,
                rs.getString("next_due_date"),
                rs.getString("performed_by"),
                rs.getString("notes"),
                rs.getString("created_at")
        );
    }

    private static void setNullableInt(PreparedStatement stmt, int index, Integer value)
            throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }
}
