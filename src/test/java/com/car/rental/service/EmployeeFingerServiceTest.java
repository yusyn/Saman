package com.car.rental.service;

import com.car.rental.api.dto.EmployeeDto;
import com.car.rental.db.EmployeeFingerRepository;
import com.car.rental.db.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "fingerprint.mock=true")
class EmployeeFingerServiceTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeFingerRepository fingerRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("EmployeeDto includes registered fingers list")
    void testEmployeeDtoFingers() throws Exception {
        String uid = "9902";
        try {
            employeeService.registerWithFingerprint(uid, "Test Enroll Finger", "09122222222", 6);
            List<Integer> fingers = employeeService.getFingersForEmployee(uid);
            assertTrue(fingers.contains(6));

            EmployeeDto dto = EmployeeDto.from(employeeService.findByDeviceUserId(uid), fingers);
            assertEquals(List.of(6), dto.getFingers());
        } finally {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM EmployeeFinger WHERE device_user_id = ?");
                 PreparedStatement ps2 = conn.prepareStatement("DELETE FROM EmployeeTable WHERE device_user_id = ?")) {
                ps.setString(1, uid);
                ps.executeUpdate();
                ps2.setString(1, uid);
                ps2.executeUpdate();
            }
        }
    }
}
