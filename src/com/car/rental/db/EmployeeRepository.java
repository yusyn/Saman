package com.car.rental.db;

import com.car.rental.model.Employee;
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
 * Persistence for employees (EmployeeTable).
 */
@Repository
public class EmployeeRepository {

    private static final int DEVICE_USER_ID_START = 1001;

    private final DataSource dataSource;

    public EmployeeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public String getNextDeviceUserId() throws SQLException {
        String sql = "SELECT device_user_id FROM EmployeeTable";
        int max = DEVICE_USER_ID_START - 1;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String raw = rs.getString("device_user_id");
                if (raw == null) continue;
                try {
                    int v = Integer.parseInt(raw.trim());
                    if (v > max) {
                        max = v;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int next = Math.max(max + 1, DEVICE_USER_ID_START);
        if (next > 65535) {
            throw new SQLException("Device user id range exhausted (max 65535)");
        }
        return String.valueOf(next);
    }

    public void addEmployee(String deviceUserId, String name, String phone) throws SQLException {
        String sql = "INSERT INTO EmployeeTable(device_user_id, name, phone) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            stmt.setString(2, name);
            stmt.setString(3, phone);
            stmt.executeUpdate();
        }
    }

    public Employee findByDeviceUserId(String deviceUserId) throws SQLException {
        String sql = "SELECT id, device_user_id, name, phone, is_active, is_renting " +
                "FROM EmployeeTable WHERE device_user_id = ? AND is_active = 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapEmployee(rs);
                }
                return null;
            }
        }
    }

    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT id, device_user_id, name, phone, is_active, is_renting " +
                "FROM EmployeeTable WHERE is_active = 1 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapEmployee(rs));
            }
        }
        return list;
    }

    public void updateEmployee(Employee emp) throws SQLException {
        String sql = "UPDATE EmployeeTable SET name = ?, phone = ?, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, emp.getName());
            stmt.setString(2, emp.getPhone());
            stmt.setInt(3, emp.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteEmployeeByDeviceUserId(String deviceUserId) throws SQLException {
        String sql = "UPDATE EmployeeTable SET is_active = 0, " +
                "updated_at = datetime('now','localtime') WHERE device_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            stmt.executeUpdate();
        }
    }

    public boolean isDeviceUserIdExists(String deviceUserId) throws SQLException {
        String sql = "SELECT 1 FROM EmployeeTable WHERE device_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Employee mapEmployee(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("id"),
                rs.getString("device_user_id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getInt("is_active") == 1,
                rs.getInt("is_renting") == 1
        );
    }
}
