package com.car.rental.db;

import com.car.rental.model.Car;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for fleet / cars (CarTable).
 */
@Repository
public class CarRepository {

    private final DataSource dataSource;

    public CarRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int getCarIdByPlate(String plate) throws SQLException {
        String sql = "SELECT id FROM CarTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("ماشین فعالی با این پلاک یافت نشد: " + plate);
                }
            }
        }
    }

    public boolean isPlateTaken(String plate, String excludePlate) throws SQLException {
        if (plate == null || plate.isBlank()) {
            return false;
        }
        if (excludePlate != null && !excludePlate.isBlank() && excludePlate.equals(plate)) {
            return false;
        }
        String sql = "SELECT 1 FROM CarTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return excludePlate == null || !excludePlate.equals(plate);
            }
        }
    }

    public List<Car> listAvailableCars() throws SQLException {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT name, color, plate FROM CarTable WHERE is_deleted = 0 AND is_rented = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cars.add(new Car(
                        rs.getString("name"),
                        rs.getString("plate"),
                        rs.getString("color"),
                        "آزاد"
                ));
            }
        }
        return cars;
    }

    public List<Car> listAllCars() throws SQLException {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT name, color, plate, is_rented FROM CarTable WHERE is_deleted = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean rented = rs.getInt("is_rented") == 1;
                cars.add(new Car(
                        rs.getString("name"),
                        rs.getString("plate"),
                        rs.getString("color"),
                        rented ? "در مأموریت" : "آزاد"
                ));
            }
        }
        return cars;
    }

    public void addCar(String name, String plate, String color) throws SQLException {
        String sql = "INSERT INTO CarTable(name, plate, color) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, plate);
            stmt.setString(3, color);
            stmt.executeUpdate();
        }
    }

    public void updateCar(Car car, String oldPlate) throws SQLException {
        String sql = "UPDATE CarTable SET name = ?, color = ?, plate = ? " +
                "WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, car.getModel());
            stmt.setString(2, car.getColor());
            stmt.setString(3, car.getPlate());
            stmt.setString(4, oldPlate);
            int n = stmt.executeUpdate();
            if (n == 0) {
                throw new SQLException("ماشین فعالی برای به‌روزرسانی یافت نشد");
            }
        }
    }

    public void deleteCar(String plate) throws SQLException {
        String checkSql = "SELECT is_rented FROM CarTable WHERE plate = ? AND is_deleted = 0";
        String sql = "UPDATE CarTable SET is_deleted = 1 WHERE plate = ? AND is_deleted = 0 AND is_rented = 0";
        try (Connection conn = getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, plate);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("ماشین فعالی یافت نشد");
                    }
                    if (rs.getInt("is_rented") == 1) {
                        throw new SQLException("ماشین در مأموریت است و قابل حذف نیست");
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, plate);
                int n = stmt.executeUpdate();
                if (n == 0) {
                    throw new SQLException("حذف ماشین انجام نشد");
                }
            }
        }
    }
}
