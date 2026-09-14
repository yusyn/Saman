package com.car.rental.db;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates SQLite tables if missing. Called once at application startup.
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

        String carTable =
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
                "FOREIGN KEY(employee_id) REFERENCES EmployeeTable(id) ON UPDATE CASCADE, " +
                "FOREIGN KEY(car_id) REFERENCES CarTable(id) ON UPDATE CASCADE" +
                ")";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(carTable);
            stmt.execute(rentalTable);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database init failed!", e);
        }
    }
}
