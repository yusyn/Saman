package com.car.rental.db;

import com.car.rental.model.Attachment;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class AttachmentRepository {

    private final DataSource dataSource;

    public AttachmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Attachment insert(Attachment attachment) throws SQLException {
        String sql = "INSERT INTO Attachment (original_name, file_path, content_type, file_size) VALUES (?, ?, ?, ?)";
        int id = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, attachment.getOriginalName());
            stmt.setString(2, attachment.getFilePath());
            stmt.setString(3, attachment.getContentType());
            stmt.setLong(4, attachment.getFileSize());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
        }
        if (id > 0) {
            return findById(id);
        }
        return attachment;
    }

    public Attachment findById(int id) throws SQLException {
        String sql = "SELECT id, original_name, file_path, content_type, file_size, created_at FROM Attachment WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Attachment(
                            rs.getInt("id"),
                            rs.getString("original_name"),
                            rs.getString("file_path"),
                            rs.getString("content_type"),
                            rs.getLong("file_size"),
                            rs.getString("created_at")
                    );
                }
            }
        }
        return null;
    }
}
