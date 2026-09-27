package com.car.rental.db;

import com.car.rental.model.VehicleFine;
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
 * Persistence for vehicle fine / violation history (VehicleFine).
 */
@Repository
public class VehicleFineRepository {

    private final DataSource dataSource;

    public VehicleFineRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int insert(VehicleFine fine) throws SQLException {
        String sql = "INSERT INTO VehicleFine(" +
                "vehicle_id, employee_id, fine_type, amount, fine_date, due_date, description) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, fine.getVehicleId());
            setNullableInt(stmt, 2, fine.getEmployeeId());
            stmt.setString(3, fine.getFineType());
            stmt.setDouble(4, fine.getAmount());
            stmt.setString(5, fine.getFineDate());
            stmt.setString(6, fine.getDueDate());
            stmt.setString(7, fine.getDescription());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        }
    }

    public List<VehicleFine> findByVehicleId(int vehicleId) throws SQLException {
        List<VehicleFine> list = new ArrayList<>();
        String sql = "SELECT id, vehicle_id, employee_id, fine_type, amount, fine_date, " +
                "due_date, is_paid, payment_date, description, created_at " +
                "FROM VehicleFine WHERE vehicle_id = ? ORDER BY fine_date DESC, id DESC";
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

    public VehicleFine findById(int id) throws SQLException {
        String sql = "SELECT id, vehicle_id, employee_id, fine_type, amount, fine_date, " +
                "due_date, is_paid, payment_date, description, created_at " +
                "FROM VehicleFine WHERE id = ?";
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

    public void markPaid(int fineId, String paymentDate) throws SQLException {
        String sql = "UPDATE VehicleFine SET is_paid = 1, payment_date = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paymentDate);
            stmt.setInt(2, fineId);
            int n = stmt.executeUpdate();
            if (n == 0) {
                throw new SQLException("خلافی با شناسه " + fineId + " یافت نشد");
            }
        }
    }

    public boolean hasUnpaid(int vehicleId) throws SQLException {
        String sql = "SELECT 1 FROM VehicleFine WHERE vehicle_id = ? AND is_paid = 0 LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public double sumAmountByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM VehicleFine WHERE vehicle_id = ?";
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

    public double sumUnpaidByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM VehicleFine " +
                "WHERE vehicle_id = ? AND is_paid = 0";
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

    private static VehicleFine map(ResultSet rs) throws SQLException {
        Integer employeeId = (Integer) rs.getObject("employee_id");
        return new VehicleFine(
                rs.getInt("id"),
                rs.getInt("vehicle_id"),
                employeeId,
                rs.getString("fine_type"),
                rs.getDouble("amount"),
                rs.getString("fine_date"),
                rs.getString("due_date"),
                rs.getInt("is_paid") == 1,
                rs.getString("payment_date"),
                rs.getString("description"),
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
