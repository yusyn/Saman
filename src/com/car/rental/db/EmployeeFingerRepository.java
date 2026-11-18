package com.car.rental.db;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence for employee enrolled fingers (EmployeeFinger table).
 */
@Repository
public class EmployeeFingerRepository {

    private final DataSource dataSource;

    public EmployeeFingerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void addFinger(String deviceUserId, int fingerIndex) throws SQLException {
        try (Connection conn = getConnection()) {
            addFinger(conn, deviceUserId, fingerIndex);
        }
    }

    public void addFinger(Connection conn, String deviceUserId, int fingerIndex) throws SQLException {
        String sql = "INSERT OR IGNORE INTO EmployeeFinger (device_user_id, finger_index) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            stmt.setInt(2, fingerIndex);
            stmt.executeUpdate();
        }
    }

    public List<Integer> getFingersByDeviceUserId(String deviceUserId) throws SQLException {
        try (Connection conn = getConnection()) {
            return getFingersByDeviceUserId(conn, deviceUserId);
        }
    }

    public List<Integer> getFingersByDeviceUserId(Connection conn, String deviceUserId) throws SQLException {
        String sql = "SELECT finger_index FROM EmployeeFinger WHERE device_user_id = ? ORDER BY finger_index";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Integer> fingers = new ArrayList<>();
                while (rs.next()) {
                    fingers.add(rs.getInt("finger_index"));
                }
                return fingers;
            }
        }
    }

    public Map<String, List<Integer>> getAllFingersGrouped() throws SQLException {
        try (Connection conn = getConnection()) {
            return getAllFingersGrouped(conn);
        }
    }

    public Map<String, List<Integer>> getAllFingersGrouped(Connection conn) throws SQLException {
        String sql = "SELECT device_user_id, finger_index FROM EmployeeFinger ORDER BY device_user_id, finger_index";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            Map<String, List<Integer>> map = new LinkedHashMap<>();
            while (rs.next()) {
                String uid = rs.getString("device_user_id");
                int f = rs.getInt("finger_index");
                map.computeIfAbsent(uid, k -> new ArrayList<>()).add(f);
            }
            return map;
        }
    }

    public void deleteFingersByDeviceUserId(Connection conn, String deviceUserId) throws SQLException {
        String sql = "DELETE FROM EmployeeFinger WHERE device_user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            stmt.executeUpdate();
        }
    }

    public void deleteFingersByDeviceUserId(String deviceUserId) throws SQLException {
        try (Connection conn = getConnection()) {
            deleteFingersByDeviceUserId(conn, deviceUserId);
        }
    }
}
