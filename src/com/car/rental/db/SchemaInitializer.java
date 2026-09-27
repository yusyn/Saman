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
                "is_rented INTEGER DEFAULT 0, " +
                "current_odometer INTEGER DEFAULT 0, " +
                "last_service_odometer INTEGER, " +
                "last_service_date TEXT, " +
                "status_flags TEXT DEFAULT ''" +
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

        // Vehicle service / maintenance history
        String vehicleServiceTable =
                "CREATE TABLE IF NOT EXISTS VehicleService (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "vehicle_id INTEGER NOT NULL, " +
                "service_type TEXT NOT NULL, " +
                "description TEXT, " +
                "odometer INTEGER, " +
                "cost REAL DEFAULT 0, " +
                "service_date TEXT NOT NULL, " +
                "next_due_odometer INTEGER, " +
                "next_due_date TEXT, " +
                "performed_by TEXT, " +
                "notes TEXT, " +
                "created_at TEXT DEFAULT (datetime('now','localtime')), " +
                "FOREIGN KEY(vehicle_id) REFERENCES VehicleTable(id)" +
                ")";

        // Vehicle damage / issue history
        String vehicleIssueTable =
                "CREATE TABLE IF NOT EXISTS VehicleIssue (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "vehicle_id INTEGER NOT NULL, " +
                "reported_by INTEGER, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "severity TEXT DEFAULT 'MEDIUM', " +
                "status TEXT DEFAULT 'OPEN', " +
                "cost REAL DEFAULT 0, " +
                "reported_at TEXT NOT NULL, " +
                "resolved_at TEXT, " +
                "notes TEXT, " +
                "created_at TEXT DEFAULT (datetime('now','localtime')), " +
                "FOREIGN KEY(vehicle_id) REFERENCES VehicleTable(id), " +
                "FOREIGN KEY(reported_by) REFERENCES EmployeeTable(id)" +
                ")";

        // Vehicle fine / violation history
        String vehicleFineTable =
                "CREATE TABLE IF NOT EXISTS VehicleFine (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "vehicle_id INTEGER NOT NULL, " +
                "employee_id INTEGER, " +
                "fine_type TEXT, " +
                "amount REAL NOT NULL, " +
                "fine_date TEXT NOT NULL, " +
                "due_date TEXT, " +
                "is_paid INTEGER DEFAULT 0, " +
                "payment_date TEXT, " +
                "description TEXT, " +
                "created_at TEXT DEFAULT (datetime('now','localtime')), " +
                "FOREIGN KEY(vehicle_id) REFERENCES VehicleTable(id), " +
                "FOREIGN KEY(employee_id) REFERENCES EmployeeTable(id)" +
                ")";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(vehicleTable);
            stmt.execute(carTableLegacy);
            stmt.execute(rentalTable);
            stmt.execute(vehicleServiceTable);
            stmt.execute(vehicleIssueTable);
            stmt.execute(vehicleFineTable);

            migrateCarTableToVehicleTable(conn);
            ensureVehicleTypeColumn(conn);
            ensureVehicleHistoryColumns(conn);
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
        ensureColumn(conn, "VehicleTable", "vehicle_type",
                "ALTER TABLE VehicleTable ADD COLUMN vehicle_type TEXT NOT NULL DEFAULT 'CAR'");
    }

    /**
     * Add history-related columns on VehicleTable for existing databases
     * that were created before vehicle history support.
     */
    private void ensureVehicleHistoryColumns(Connection conn) throws SQLException {
        ensureColumn(conn, "VehicleTable", "current_odometer",
                "ALTER TABLE VehicleTable ADD COLUMN current_odometer INTEGER DEFAULT 0");
        ensureColumn(conn, "VehicleTable", "last_service_odometer",
                "ALTER TABLE VehicleTable ADD COLUMN last_service_odometer INTEGER");
        ensureColumn(conn, "VehicleTable", "last_service_date",
                "ALTER TABLE VehicleTable ADD COLUMN last_service_date TEXT");
        ensureColumn(conn, "VehicleTable", "status_flags",
                "ALTER TABLE VehicleTable ADD COLUMN status_flags TEXT DEFAULT ''");
    }

    private void ensureColumn(Connection conn, String table, String column, String alterSql)
            throws SQLException {
        boolean hasColumn = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    hasColumn = true;
                    break;
                }
            }
        }
        if (!hasColumn) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                logger.info("Added column " + column + " to " + table);
            }
        }
    }
}
