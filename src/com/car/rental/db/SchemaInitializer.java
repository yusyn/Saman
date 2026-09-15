package com.car.rental.db;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates SQLite tables if missing and migrates schema. Called once at application startup.
 */
@Component
public class SchemaInitializer {

    private static final Logger logger = Logger.getLogger(SchemaInitializer.class.getName());

    private final DataSource dataSource;

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initDatabase() {
        String employeeTable =
                "CREATE TABLE IF NOT EXISTS EmployeeTable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "device_user_id TEXT NOT NULL UNIQUE, " +
                "name TEXT NOT NULL, " +
                "phone TEXT, " +
                "is_active INTEGER DEFAULT 1, " +
                "is_renting INTEGER DEFAULT 0, " +
                "created_at TEXT DEFAULT (datetime('now','localtime')), " +
                "updated_at TEXT DEFAULT (datetime('now','localtime'))" +
                ")";

        // New canonical table name
        String vehicleTable =
                "CREATE TABLE IF NOT EXISTS VehicleTable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "plate TEXT NOT NULL, " +
                "color TEXT NOT NULL, " +
                "vehicle_type TEXT NOT NULL DEFAULT 'CAR', " +
                "is_deleted INTEGER DEFAULT 0, " +
                "is_rented INTEGER DEFAULT 0" +
                ")";

        // Legacy CarTable (kept for migration from older installs)
        String carTableLegacy =
                "CREATE TABLE IF NOT EXISTS CarTable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "plate TEXT NOT NULL, " +
                "color TEXT NOT NULL, " +
                "is_deleted INTEGER DEFAULT 0, " +
                "is_rented INTEGER DEFAULT 0" +
                ")";

        String rentalTable =
                "CREATE TABLE IF NOT EXISTS RentalTable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "employee_id INTEGER NOT NULL, " +
                "car_id INTEGER NOT NULL, " +
                "pickup_date TEXT, " +
                "return_date TEXT, " +
                "destination TEXT NOT NULL, " +
                "is_active INTEGER DEFAULT 1, " +
                "FOREIGN KEY(employee_id) REFERENCES EmployeeTable(id) ON UPDATE CASCADE" +
                ")";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(vehicleTable);
            stmt.execute(carTableLegacy);
            stmt.execute(rentalTable);

            migrateCarTableToVehicleTable(conn);
            ensureVehicleTypeColumn(conn);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database init failed!", e);
        }
    }

    /** One-time copy from legacy CarTable into VehicleTable if VehicleTable is empty. */
    private void migrateCarTableToVehicleTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) AS c FROM VehicleTable")) {
            if (countRs.next() && countRs.getInt("c") > 0) {
                return;
            }
        }
        try (Statement stmt = conn.createStatement();
             ResultSet exists = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='CarTable'")) {
            if (!exists.next()) {
                return;
            }
        }
        try (Statement stmt = conn.createStatement()) {
            int n = stmt.executeUpdate(
                    "INSERT INTO VehicleTable (name, plate, color, vehicle_type, is_deleted, is_rented) " +
                    "SELECT name, plate, color, 'CAR', is_deleted, is_rented FROM CarTable");
            if (n > 0) {
                logger.info("Migrated " + n + " row(s) from CarTable to VehicleTable");
            }
        }
    }

    /** Add vehicle_type if an older VehicleTable was created without it. */
    private void ensureVehicleTypeColumn(Connection conn) throws SQLException {
        boolean hasColumn = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(VehicleTable)")) {
            while (rs.next()) {
                if ("vehicle_type".equalsIgnoreCase(rs.getString("name"))) {
                    hasColumn = true;
                    break;
                }
            }
        }
        if (!hasColumn) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE VehicleTable ADD COLUMN vehicle_type TEXT NOT NULL DEFAULT 'CAR'");
                logger.info("Added vehicle_type column to VehicleTable");
            }
        }
    }
}
