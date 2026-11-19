package com.car.rental.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class VehicleMissionReportSchemaTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify new schema tables and columns are initialized")
    void testNewSchemaInitialized() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check tables exist
            Set<String> tables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                while (rs.next()) {
                    tables.add(rs.getString("name").toLowerCase());
                }
            }

            assertTrue(tables.contains("attachment"), "Attachment table should exist");
            assertTrue(tables.contains("vehiclemissionreport"), "VehicleMissionReport table should exist");
            assertTrue(tables.contains("vehicledocument"), "VehicleDocument table should exist");

            // Check RentalTable return_odometer
            Set<String> rentalCols = getColumns(conn, "RentalTable");
            assertTrue(rentalCols.contains("return_odometer"), "RentalTable should have return_odometer column");

            // Check EmployeeTable driver license columns
            Set<String> empCols = getColumns(conn, "EmployeeTable");
            assertTrue(empCols.contains("driver_license_number"), "EmployeeTable should have driver_license_number");
            assertTrue(empCols.contains("driver_license_expiry"), "EmployeeTable should have driver_license_expiry");
        }
    }

    private Set<String> getColumns(Connection conn, String table) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                cols.add(rs.getString("name").toLowerCase());
            }
        }
        return cols;
    }
}
