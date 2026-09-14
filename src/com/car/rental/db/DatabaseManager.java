package com.car.rental.db;

import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade over domain repositories — keeps existing service constructors working
 * while SQL lives in EmployeeRepository, CarRepository, and RentalRepository.
 */
@Repository
public class DatabaseManager {

    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private final DataSource dataSource;
    private final EmployeeRepository employees;
    private final CarRepository cars;
    private final RentalRepository rentals;

    public DatabaseManager(DataSource dataSource,
                           EmployeeRepository employees,
                           CarRepository cars,
                           RentalRepository rentals) {
        this.dataSource = dataSource;
        this.employees = employees;
        this.cars = cars;
        this.rentals = rentals;
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

    // --- employee ---

    public String getNextDeviceUserId() throws SQLException {
        return employees.getNextDeviceUserId();
    }

    public void addEmployee(String deviceUserId, String name, String phone) throws SQLException {
        employees.addEmployee(deviceUserId, name, phone);
    }

    public Employee findByDeviceUserId(String deviceUserId) throws SQLException {
        return employees.findByDeviceUserId(deviceUserId);
    }

    public List<Employee> getAllEmployees() throws SQLException {
        return employees.getAllEmployees();
    }

    public void updateEmployee(Employee emp) throws SQLException {
        employees.updateEmployee(emp);
    }

    public void deleteEmployeeByDeviceUserId(String deviceUserId) throws SQLException {
        employees.deleteEmployeeByDeviceUserId(deviceUserId);
    }

    public boolean isDeviceUserIdExists(String deviceUserId) throws SQLException {
        return employees.isDeviceUserIdExists(deviceUserId);
    }

    // --- car ---

    public int getCarIdByPlate(String plate) throws SQLException {
        return cars.getCarIdByPlate(plate);
    }

    public boolean isPlateTaken(String plate, String excludePlate) throws SQLException {
        return cars.isPlateTaken(plate, excludePlate);
    }

    public List<Car> listAvailableCars() throws SQLException {
        return cars.listAvailableCars();
    }

    public List<Car> listAllCars() throws SQLException {
        return cars.listAllCars();
    }

    public void addCar(String name, String plate, String color) throws SQLException {
        cars.addCar(name, plate, color);
    }

    public void updateCar(Car car, String oldPlate) throws SQLException {
        cars.updateCar(car, oldPlate);
    }

    public void deleteCar(String plate) throws SQLException {
        cars.deleteCar(plate);
    }

    // --- rental ---

    public void insertRental(String deviceUserId, String carPlate, String pickupTime, String destination)
            throws SQLException {
        rentals.insertRental(deviceUserId, carPlate, pickupTime, destination);
    }

    public boolean returnCarByDeviceUserId(String deviceUserId, String returnDate) throws SQLException {
        return rentals.returnCarByDeviceUserId(deviceUserId, returnDate);
    }

    public List<RentalRecord> getRentalReport() throws SQLException {
        return rentals.getRentalReport();
    }

    public List<RentalRecord> getRentalReport(RentalReportFilter filter) throws SQLException {
        return rentals.getRentalReport(filter);
    }

    public RentalRecord getActiveRentalByDeviceUserId(String deviceUserId) throws SQLException {
        return rentals.getActiveRentalByDeviceUserId(deviceUserId);
    }
}
