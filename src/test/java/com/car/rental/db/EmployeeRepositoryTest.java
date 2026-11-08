package com.car.rental.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("getNextDeviceUserId returns at least 1001 and increments based on max numeric ID")
    void testNextDeviceUserId() throws SQLException {
        String nextId = employeeRepository.getNextDeviceUserId();
        assertNotNull(nextId);
        int id = Integer.parseInt(nextId);
        assertTrue(id >= 1001, "Device User ID should be >= 1001");
    }

    @Test
    @DisplayName("getNextDeviceUserId properly tracks numeric IDs and ignores non-numeric IDs")
    void testNextDeviceUserIdCalculations() throws SQLException {
        String baseIdStr = employeeRepository.getNextDeviceUserId();
        int baseId = Integer.parseInt(baseIdStr);

        String testNumericId = String.valueOf(baseId + 10);
        String testAlphaId = "TEMP_ALPHA_" + System.currentTimeMillis();

        try {
            employeeRepository.addEmployee(testAlphaId, "Alpha Test", "09120000001");
            // Alpha ID should not change next ID
            assertEquals(String.valueOf(baseId), employeeRepository.getNextDeviceUserId(),
                    "Non-numeric device_user_id must not affect next ID calculation");

            employeeRepository.addEmployee(testNumericId, "Numeric Test", "09120000002");
            // Numeric ID should update next ID
            assertEquals(String.valueOf(baseId + 11), employeeRepository.getNextDeviceUserId(),
                    "Next ID should be highest numeric ID + 1");
        } finally {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM EmployeeTable WHERE device_user_id IN (?, ?)")) {
                ps.setString(1, testAlphaId);
                ps.setString(2, testNumericId);
                ps.executeUpdate();
            }
        }
    }
}
