package com.car.rental.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ForeignKeyEnforcementTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("PRAGMA foreign_keys returns 1 on all connections")
    void testForeignKeysPragmaIsEnabled() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("PRAGMA foreign_keys");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "SQLite foreign keys pragma must be 1 (enabled)");
        }
    }

    @Test
    @DisplayName("Inserting rental for non-existent vehicle throws foreign key constraint violation")
    void testForeignKeyConstraintViolation() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // car_id 999999 does not exist
            String sql = "INSERT INTO RentalTable(employee_id, car_id, destination) VALUES(1, 999999, 'Tehran')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                assertThrows(SQLException.class, ps::executeUpdate,
                        "Engine must reject row referencing non-existent vehicle");
            }
        }
    }
}
