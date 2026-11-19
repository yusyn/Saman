package com.car.rental.db;

import com.car.rental.model.VehicleDocument;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class VehicleDocumentRepository {

    private final DataSource dataSource;

    public VehicleDocumentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int insert(VehicleDocument doc) throws SQLException {
        String sql = "INSERT INTO VehicleDocument (vehicle_id, document_type, document_number, issue_date, " +
                "expiry_date, attachment_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, doc.getVehicleId());
            stmt.setString(2, doc.getDocumentType());
            stmt.setString(3, doc.getDocumentNumber());
            stmt.setString(4, doc.getIssueDate());
            stmt.setString(5, doc.getExpiryDate());
            if (doc.getAttachmentId() == null) stmt.setNull(6, Types.INTEGER);
            else stmt.setInt(6, doc.getAttachmentId());
            stmt.setString(7, doc.getNotes());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<VehicleDocument> findByVehicleId(int vehicleId) throws SQLException {
        String sql = "SELECT * FROM VehicleDocument WHERE vehicle_id = ? ORDER BY expiry_date ASC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<VehicleDocument> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM VehicleDocument WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private VehicleDocument mapRow(ResultSet rs) throws SQLException {
        return new VehicleDocument(
                rs.getInt("id"),
                rs.getInt("vehicle_id"),
                rs.getString("document_type"),
                rs.getString("document_number"),
                rs.getString("issue_date"),
                rs.getString("expiry_date"),
                (Integer) rs.getObject("attachment_id"),
                rs.getString("notes"),
                rs.getString("created_at")
        );
    }
}
