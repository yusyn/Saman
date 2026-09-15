package com.car.rental.db;

import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Persistence for rentals (RentalTable) including multi-table pickup/return transactions.
 * car_id column still points to VehicleTable.id (historical column name).
 */
@Repository
public class RentalRepository {

    private static final Logger logger = Logger.getLogger(RentalRepository.class.getName());

    private final DataSource dataSource;

    public RentalRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void insertRental(String deviceUserId,
                             String vehiclePlate,
                             String pickupTime,
                             String destination) throws SQLException {

        String empSql = "SELECT id, is_renting FROM EmployeeTable " +
                "WHERE device_user_id = ? AND is_active = 1";
        String vehicleSql = "SELECT id, is_rented FROM VehicleTable " +
                "WHERE plate = ? AND is_deleted = 0";
        String insertSql = "INSERT INTO RentalTable(employee_id, car_id, pickup_date, destination) " +
                "VALUES (?, ?, ?, ?)";
        String updateVehicleSql = "UPDATE VehicleTable SET is_rented = 1 WHERE id = ? AND is_rented = 0 AND is_deleted = 0";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 1, " +
                "updated_at = datetime('now','localtime') WHERE id = ? AND is_renting = 0";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int empId;
                try (PreparedStatement empStmt = conn.prepareStatement(empSql)) {
                    empStmt.setString(1, deviceUserId);
                    try (ResultSet rs = empStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("کارمند با شناسه دستگاه یافت نشد: " + deviceUserId);
                        }
                        if (rs.getInt("is_renting") == 1) {
                            throw new SQLException("این کارمند در حال حاضر در مأموریت است");
                        }
                        empId = rs.getInt("id");
                    }
                }

                int vehicleId;
                try (PreparedStatement vStmt = conn.prepareStatement(vehicleSql)) {
                    vStmt.setString(1, vehiclePlate);
                    try (ResultSet rs = vStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("وسیله فعالی با این پلاک یافت نشد: " + vehiclePlate);
                        }
                        if (rs.getInt("is_rented") == 1) {
                            throw new SQLException("این وسیله هم‌اکنون در مأموریت است");
                        }
                        vehicleId = rs.getInt("id");
                    }
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                     PreparedStatement updateVehicleStmt = conn.prepareStatement(updateVehicleSql);
                     PreparedStatement updateEmpStmt = conn.prepareStatement(updateEmpSql)) {

                    insertStmt.setInt(1, empId);
                    insertStmt.setInt(2, vehicleId);
                    insertStmt.setString(3, pickupTime);
                    insertStmt.setString(4, destination);
                    insertStmt.executeUpdate();

                    updateVehicleStmt.setInt(1, vehicleId);
                    int vehicleUpdated = updateVehicleStmt.executeUpdate();
                    if (vehicleUpdated != 1) {
                        throw new SQLException("به‌روزرسانی وضعیت وسیله انجام نشد");
                    }

                    updateEmpStmt.setInt(1, empId);
                    int empUpdated = updateEmpStmt.executeUpdate();
                    if (empUpdated != 1) {
                        throw new SQLException("به‌روزرسانی وضعیت کارمند انجام نشد");
                    }
                }

                conn.commit();
                logger.info("Rental inserted: empId=" + empId + " vehicleId=" + vehicleId + " plate=" + vehiclePlate);
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean returnCarByDeviceUserId(String deviceUserId, String returnDate) throws SQLException {
        String empSql = "SELECT id FROM EmployeeTable WHERE device_user_id = ? AND is_active = 1";
        String selectSql = "SELECT id, car_id FROM RentalTable " +
                "WHERE employee_id = ? AND return_date IS NULL AND is_active = 1";
        String updateRentalSql = "UPDATE RentalTable SET return_date = ?, is_active = 0 WHERE id = ?";
        String updateVehicleSql = "UPDATE VehicleTable SET is_rented = 0 WHERE id = ?";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 0, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int empId;
                try (PreparedStatement empStmt = conn.prepareStatement(empSql)) {
                    empStmt.setString(1, deviceUserId);
                    try (ResultSet rs = empStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("کارمند با شناسه دستگاه یافت نشد: " + deviceUserId);
                        }
                        empId = rs.getInt("id");
                    }
                }

                int rentalId;
                int vehicleId;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setInt(1, empId);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                        rentalId = rs.getInt("id");
                        vehicleId = rs.getInt("car_id");
                    }
                }

                try (PreparedStatement updateRental = conn.prepareStatement(updateRentalSql);
                     PreparedStatement updateVehicle = conn.prepareStatement(updateVehicleSql);
                     PreparedStatement updateEmp = conn.prepareStatement(updateEmpSql)) {

                    updateRental.setString(1, returnDate);
                    updateRental.setInt(2, rentalId);
                    updateRental.executeUpdate();

                    updateVehicle.setInt(1, vehicleId);
                    updateVehicle.executeUpdate();

                    updateEmp.setInt(1, empId);
                    updateEmp.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<RentalRecord> getRentalReport() throws SQLException {
        return getRentalReport(new RentalReportFilter());
    }

    public List<RentalRecord> getRentalReport(RentalReportFilter filter) throws SQLException {
        if (filter == null) {
            filter = new RentalReportFilter();
        }

        StringBuilder sql = new StringBuilder(
                "SELECT e.device_user_id, e.name AS employee_name, " +
                "v.name AS car_name, v.color AS car_color, v.plate, v.vehicle_type, " +
                "r.pickup_date, r.return_date, r.destination " +
                "FROM RentalTable r " +
                "JOIN EmployeeTable e ON r.employee_id = e.id " +
                "JOIN VehicleTable v ON r.car_id = v.id WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (filter.getEmployeeName() != null) {
            sql.append(" AND e.name LIKE ?");
            params.add("%" + filter.getEmployeeName() + "%");
        }
        if (filter.getPlate() != null) {
            sql.append(" AND v.plate LIKE ?");
            params.add("%" + filter.getPlate() + "%");
        }
        if (filter.getCarName() != null) {
            sql.append(" AND v.name LIKE ?");
            params.add("%" + filter.getCarName() + "%");
        }
        if (filter.getDestination() != null) {
            sql.append(" AND r.destination LIKE ?");
            params.add("%" + filter.getDestination() + "%");
        }

        switch (filter.getStatus()) {
            case OPEN:
                sql.append(" AND r.return_date IS NULL AND r.is_active = 1");
                break;
            case CLOSED:
                sql.append(" AND r.return_date IS NOT NULL");
                break;
            default:
                break;
        }

        String from = filter.getDateFrom();
        String to = filter.getDateTo();
        if (from != null || to != null) {
            if (from == null) {
                from = to;
            }
            if (to == null) {
                to = from;
            }
            String rangeStart = from + " 00:00:00";
            String rangeEnd = to + " 23:59:59";
            sql.append(" AND r.pickup_date IS NOT NULL");
            sql.append(" AND r.pickup_date <= ?");
            params.add(rangeEnd);
            sql.append(" AND (r.return_date IS NULL OR r.return_date >= ?)");
            params.add(rangeStart);
        }

        sql.append(" ORDER BY r.pickup_date DESC");

        List<RentalRecord> records = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRentalRecord(rs));
                }
            }
        }
        return records;
    }

    private static RentalRecord mapRentalRecord(ResultSet rs) throws SQLException {
        RentalRecord r = new RentalRecord(
                rs.getString("device_user_id"),
                rs.getString("employee_name"),
                rs.getString("car_name"),
                rs.getString("car_color"),
                rs.getString("plate"),
                rs.getString("pickup_date"),
                rs.getString("return_date"),
                rs.getString("destination")
        );
        try {
            r.vehicleType = rs.getString("vehicle_type");
        } catch (SQLException ignored) {
            r.vehicleType = "CAR";
        }
        return r;
    }

    public RentalRecord getActiveRentalByDeviceUserId(String deviceUserId) throws SQLException {
        String query = "SELECT e.device_user_id, e.name AS employee_name, " +
                "v.name AS car_name, v.color AS car_color, v.plate, v.vehicle_type, " +
                "r.pickup_date, r.return_date, r.destination " +
                "FROM RentalTable r " +
                "JOIN EmployeeTable e ON r.employee_id = e.id " +
                "JOIN VehicleTable v ON r.car_id = v.id " +
                "WHERE e.device_user_id = ? AND r.return_date IS NULL AND r.is_active = 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, deviceUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRentalRecord(rs);
                }
                return null;
            }
        }
    }
}
