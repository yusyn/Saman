package com.car.rental.api;

import com.car.rental.api.dto.EmployeeDto;
import com.car.rental.api.dto.SyncFingerprintsResponse;
import com.car.rental.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "fingerprint.mock=true")
class EmployeeControllerSyncTest {

    @Autowired
    private EmployeeController employeeController;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DataSource dataSource;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("syncFingerprints imports missing fingers into SQLite and returns summary")
    void testSyncFingerprints() throws Exception {
        String uid = "9903";
        try {
            // Register employee through service
            employeeService.registerWithFingerprint(uid, "Sync User", "09123333333", 6);

            // Manually delete the finger from DB to simulate legacy/device-only state
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM EmployeeFinger WHERE device_user_id = ?")) {
                ps.setString(1, uid);
                ps.executeUpdate();
            }

            // Verify DB currently has 0 fingers for this user
            assertEquals(0, employeeService.getFingersForEmployee(uid).size());

            // Run sync
            SyncFingerprintsResponse res = employeeController.syncFingerprints();
            assertNotNull(res);
            assertTrue(res.getScannedEmployees() >= 1);

            // Verify finger was restored into DB
            List<Integer> restored = employeeService.getFingersForEmployee(uid);
            assertTrue(restored.contains(6));

            // Verify EmployeeDto returned by controller list has fingers populated
            List<EmployeeDto> list = employeeController.list();
            EmployeeDto userDto = list.stream().filter(e -> uid.equals(e.getDeviceUserId())).findFirst().orElse(null);
            assertNotNull(userDto);
            assertTrue(userDto.getFingers().contains(6));
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
