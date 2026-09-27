package com.car.rental.db;

import com.car.rental.model.VehicleIssue;
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
 * Persistence for vehicle damage / issue history (VehicleIssue).
 */
@Repository
public class VehicleIssueRepository {

    private final DataSource dataSource;

    public VehicleIssueRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int insert(VehicleIssue issue) throws SQLException {
        String sql = "INSERT INTO VehicleIssue(" +
                "vehicle_id, reported_by, title, description, severity, status, " +
                "cost, reported_at, notes) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, issue.getVehicleId());
            setNullableInt(stmt, 2, issue.getReportedBy());
            stmt.setString(3, issue.getTitle());
            stmt.setString(4, issue.getDescription());
            stmt.setString(5, issue.getSeverity());
            stmt.setString(6, issue.getStatus());
            stmt.setDouble(7, issue.getCost());
            stmt.setString(8, issue.getReportedAt());
            stmt.setString(9, issue.getNotes());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        }
    }

    public List<VehicleIssue> findByVehicleId(int vehicleId) throws SQLException {
        List<VehicleIssue> list = new ArrayList<>();
        String sql = "SELECT id, vehicle_id, reported_by, title, description, severity, status, " +
                "cost, reported_at, resolved_at, notes, created_at " +
                "FROM VehicleIssue WHERE vehicle_id = ? ORDER BY reported_at DESC, id DESC";
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

    public VehicleIssue findById(int id) throws SQLException {
        String sql = "SELECT id, vehicle_id, reported_by, title, description, severity, status, " +
                "cost, reported_at, resolved_at, notes, created_at " +
                "FROM VehicleIssue WHERE id = ?";
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

    public void update(VehicleIssue issue) throws SQLException {
        String sql = "UPDATE VehicleIssue SET status = ?, cost = ?, resolved_at = ?, " +
                "notes = ?, severity = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, issue.getStatus());
            stmt.setDouble(2, issue.getCost());
            stmt.setString(3, issue.getResolvedAt());
            stmt.setString(4, issue.getNotes());
            stmt.setString(5, issue.getSeverity());
            stmt.setInt(6, issue.getId());
            int n = stmt.executeUpdate();
            if (n == 0) {
                throw new SQLException("خرابی با شناسه " + issue.getId() + " یافت نشد");
            }
        }
    }

    public boolean hasOpenHighSeverity(int vehicleId) throws SQLException {
        String sql = "SELECT 1 FROM VehicleIssue WHERE vehicle_id = ? " +
                "AND status IN ('OPEN', 'IN_PROGRESS') " +
                "AND severity IN ('HIGH', 'CRITICAL') LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean hasInProgress(int vehicleId) throws SQLException {
        String sql = "SELECT 1 FROM VehicleIssue WHERE vehicle_id = ? " +
                "AND status = 'IN_PROGRESS' LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public double sumCostByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(cost), 0) AS total FROM VehicleIssue WHERE vehicle_id = ?";
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

    private static VehicleIssue map(ResultSet rs) throws SQLException {
        Integer reportedBy = (Integer) rs.getObject("reported_by");
        return new VehicleIssue(
                rs.getInt("id"),
                rs.getInt("vehicle_id"),
                reportedBy,
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getDouble("cost"),
                rs.getString("reported_at"),
                rs.getString("resolved_at"),
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
