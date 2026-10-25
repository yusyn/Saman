package com.car.rental.db;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class SystemSnapshotRepository {
    public String find(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT snapshot_value FROM SystemSnapshot WHERE snapshot_key=?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    public void save(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO SystemSnapshot(snapshot_key,snapshot_value) VALUES(?,?) " +
                        "ON CONFLICT(snapshot_key) DO UPDATE SET snapshot_value=excluded.snapshot_value, " +
                        "updated_at=datetime('now','localtime')")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }
}
