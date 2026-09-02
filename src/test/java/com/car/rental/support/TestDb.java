package com.car.rental.support;

import com.car.rental.db.DatabaseManager;
import com.car.rental.service.CarService;
import com.car.rental.service.RentalService;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Isolated SQLite file per test class/instance so tests never touch CarRental.db.
 */
public final class TestDb implements AutoCloseable {

    private final Path dbFile;
    private final DataSource dataSource;
    private final DatabaseManager databaseManager;
    private final CarService carService;
    private final RentalService rentalService;

    public TestDb() throws IOException, SQLException {
        dbFile = Files.createTempFile("saman-test-", ".db");
        String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        dataSource = new SingleUrlDataSource(url);
        databaseManager = new DatabaseManager(dataSource);
        databaseManager.initDatabase();
        carService = new CarService(databaseManager);
        rentalService = new RentalService(databaseManager);
    }

    public DatabaseManager db() {
        return databaseManager;
    }

    public CarService cars() {
        return carService;
    }

    public RentalService rentals() {
        return rentalService;
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(dbFile);
        } catch (IOException ignored) {
        }
    }

    private static final class SingleUrlDataSource implements DataSource {
        private final String url;

        SingleUrlDataSource(String url) {
            this.url = url;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("unwrap not supported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
