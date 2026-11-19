package com.car.rental.db;

import com.car.rental.model.VehicleMissionReport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class VehicleMissionReportRepository {

    private final DataSource dataSource;

    public VehicleMissionReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int insert(VehicleMissionReport r) throws SQLException {
        try (Connection conn = getConnection()) {
            return insert(conn, r);
        }
    }

    public int insert(Connection conn, VehicleMissionReport r) throws SQLException {
        String sql = "INSERT INTO VehicleMissionReport (rental_id, vehicle_id, employee_id, report_type, " +
                "title, description, amount, liters, severity, fine_type, is_paid, attachment_id, report_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInt(stmt, 1, r.getRentalId());
            stmt.setInt(2, r.getVehicleId());
            setNullableInt(stmt, 3, r.getEmployeeId());
            stmt.setString(4, r.getReportType());
            stmt.setString(5, r.getTitle());
            stmt.setString(6, r.getDescription());
            stmt.setDouble(7, r.getAmount());
            stmt.setDouble(8, r.getLiters());
            stmt.setString(9, r.getSeverity());
            stmt.setString(10, r.getFineType());
            stmt.setInt(11, r.isPaid() ? 1 : 0);
            setNullableInt(stmt, 12, r.getAttachmentId());
            stmt.setString(13, r.getReportDate());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<VehicleMissionReport> findByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM VehicleMissionReport WHERE vehicle_id = ? ORDER BY report_date DESC, id DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<VehicleMissionReport> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    public List<VehicleMissionReport> findByRentalId(int rentalId) throws SQLException {
        String sql = "SELECT * FROM VehicleMissionReport WHERE rental_id = ? ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rentalId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<VehicleMissionReport> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    public void markFinePaid(int reportId) throws SQLException {
        String sql = "UPDATE VehicleMissionReport SET is_paid = 1 WHERE id = ? AND report_type = 'FINE'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reportId);
            stmt.executeUpdate();
        }
    }

    public double sumAmountByVehicleIdAndType(int vehicleId, String type) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM VehicleMissionReport WHERE vehicle_id = ? AND report_type = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            stmt.setString(2, type);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    public double sumUnpaidFinesByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM VehicleMissionReport WHERE vehicle_id = ? AND report_type = 'FINE' AND is_paid = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    public boolean hasUnpaidFine(int vehicleId) throws SQLException {
        String sql = "SELECT 1 FROM VehicleMissionReport WHERE vehicle_id = ? AND report_type = 'FINE' AND is_paid = 0 LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean hasOpenAccident(int vehicleId) throws SQLException {
        String sql = "SELECT 1 FROM VehicleMissionReport WHERE vehicle_id = ? AND report_type = 'ACCIDENT' LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void setNullableInt(PreparedStatement stmt, int idx, Integer val) throws SQLException {
        if (val == null) stmt.setNull(idx, Types.INTEGER);
        else stmt.setInt(idx, val);
    }

    private VehicleMissionReport mapRow(ResultSet rs) throws SQLException {
        return new VehicleMissionReport(
                rs.getInt("id"),
                (Integer) rs.getObject("rental_id"),
                rs.getInt("vehicle_id"),
                (Integer) rs.getObject("employee_id"),
                rs.getString("report_type"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getDouble("amount"),
                rs.getDouble("liters"),
                rs.getString("severity"),
                rs.getString("fine_type"),
                rs.getInt("is_paid") == 1,
                (Integer) rs.getObject("attachment_id"),
                rs.getString("report_date"),
                rs.getString("created_at")
        );
    }
}
