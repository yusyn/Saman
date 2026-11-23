package com.car.rental.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeFingerRepositoryTest {

    @Autowired
    private EmployeeFingerRepository repository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("addFinger and getFingersByDeviceUserId store and retrieve unique finger indexes")
    void testAddAndRetrieveFingers() throws SQLException {
        String testUserId = "9901";
        try {
            employeeRepository.addEmployee(testUserId, "Finger Test User", "09121111111");

            repository.addFinger(testUserId, 6);
            repository.addFinger(testUserId, 5);
            // Idempotent add: adding same index should not duplicate
            repository.addFinger(testUserId, 6);

            List<Integer> fingers = repository.getFingersByDeviceUserId(testUserId);
            assertEquals(2, fingers.size());
            assertTrue(fingers.contains(5));
            assertTrue(fingers.contains(6));

            Map<String, List<Integer>> grouped = repository.getAllFingersGrouped();
            assertTrue(grouped.containsKey(testUserId));
            assertEquals(2, grouped.get(testUserId).size());
        } finally {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM EmployeeFinger WHERE device_user_id = ?");
                 PreparedStatement ps2 = conn.prepareStatement("DELETE FROM EmployeeTable WHERE device_user_id = ?")) {
                ps.setString(1, testUserId);
                ps.executeUpdate();
                ps2.setString(1, testUserId);
                ps2.executeUpdate();
            }
        }
    }

    @Test
    @DisplayName("deleteFinger removes only the targeted finger index")
    void testDeleteSingleFinger() throws Exception {
        String testUserId = "9902";
        try {
            employeeRepository.addEmployee(testUserId, "Finger Delete Test", "09122222222");
            repository.addFinger(testUserId, 0);
            repository.addFinger(testUserId, 1);
            repository.deleteFinger(testUserId, 0);
            List<Integer> remaining = repository.getFingersByDeviceUserId(testUserId);
            assertEquals(List.of(1), remaining);
        } finally {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM EmployeeFinger WHERE device_user_id = ?");
                 PreparedStatement ps2 = conn.prepareStatement("DELETE FROM EmployeeTable WHERE device_user_id = ?")) {
                ps.setString(1, testUserId);
                ps.executeUpdate();
                ps2.setString(1, testUserId);
                ps2.executeUpdate();
            }
        }
    }
}
