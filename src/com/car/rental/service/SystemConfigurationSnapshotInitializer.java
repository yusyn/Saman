package com.car.rental.service;

import com.car.rental.SamanApplication;
import com.car.rental.config.FingerprintProperties;
import com.car.rental.db.SystemSnapshotRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Captures runtime configuration changes atomically with the system event that records their detection. */
@Component
@Order(200)
public class SystemConfigurationSnapshotInitializer implements ApplicationRunner {
    private static final Logger log = Logger.getLogger(SystemConfigurationSnapshotInitializer.class.getName());
    private static final String DEVICE_HOST_KEY = "fingerprint.host";
    private static final String DEVICE_PORT_KEY = "fingerprint.port";
    private static final String VERSION_KEY = "application.version";

    private final DataSource dataSource;
    private final FingerprintProperties fingerprintProperties;
    private final SystemSnapshotRepository snapshots;
    private final SystemEventService events;
    private final ObjectProvider<BuildProperties> buildProperties;

    public SystemConfigurationSnapshotInitializer(DataSource dataSource,
                                                  FingerprintProperties fingerprintProperties,
                                                  SystemSnapshotRepository snapshots,
                                                  SystemEventService events,
                                                  ObjectProvider<BuildProperties> buildProperties) {
        this.dataSource = dataSource;
        this.fingerprintProperties = fingerprintProperties;
        this.snapshots = snapshots;
        this.events = events;
        this.buildProperties = buildProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String version = applicationVersion();
        if (version == null) log.warning("Application version metadata is unavailable; version snapshot was skipped");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                syncDeviceAddress(connection);
                if (version != null) syncValue(connection, VERSION_KEY, version,
                        "نسخهٔ نرم‌افزار", "تغییر نسخهٔ نرم‌افزار", "نسخهٔ نرم‌افزار هنگام راه‌اندازی شناسایی شد");
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                throw failure;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("ثبت snapshot تنظیمات سیستم هنگام راه‌اندازی انجام نشد", e);
        }
    }

    private void syncDeviceAddress(Connection connection) throws SQLException {
        String host = fingerprintProperties.getHost() == null ? "" : fingerprintProperties.getHost().strip();
        String port = Integer.toString(fingerprintProperties.getPort());
        String oldHost = snapshots.find(connection, DEVICE_HOST_KEY);
        String oldPort = snapshots.find(connection, DEVICE_PORT_KEY);
        boolean hasBaseline = oldHost != null || oldPort != null;
        boolean changed = (oldHost != null && !oldHost.equals(host)) || (oldPort != null && !oldPort.equals(port));

        if (changed) {
            Map<String, Object> oldValues = new LinkedHashMap<>();
            if (oldHost != null) oldValues.put("host", oldHost);
            if (oldPort != null) oldValues.put("port", oldPort);
            Map<String, Object> newValues = Map.of("host", host, "port", Integer.parseInt(port));
            events.recordAs(connection, "سامانه", "تنظیمات سامانه", "تغییر آدرس دستگاه اثرانگشت", "موفق",
                    "آدرس IP/پورت دستگاه هنگام راه‌اندازی شناسایی شد؛ زمان رویداد زمان شناسایی است",
                    "تنظیمات", "fingerprint.address", Map.of("old", oldValues, "new", newValues));
        } else if (!hasBaseline) {
            log.info("Fingerprint address baseline captured");
        }

        snapshots.save(connection, DEVICE_HOST_KEY, host);
        snapshots.save(connection, DEVICE_PORT_KEY, port);
    }

    private void syncValue(Connection connection, String key, String value, String category,
                           String eventType, String description) throws SQLException {
        String previous = snapshots.find(connection, key);
        if (previous != null && !previous.equals(value)) {
            events.recordAs(connection, "سامانه", category, eventType, "موفق", description + "؛ زمان رویداد زمان شناسایی است",
                    "تنظیمات", key, Map.of("old", previous, "new", value));
        } else if (previous == null) {
            log.info("Application version baseline captured");
        }
        snapshots.save(connection, key, value);
    }

    private String applicationVersion() {
        BuildProperties build = buildProperties.getIfAvailable();
        if (build != null && isUsable(build.getVersion())) return build.getVersion().strip();

        Package appPackage = SamanApplication.class.getPackage();
        if (appPackage != null && isUsable(appPackage.getImplementationVersion())) {
            return appPackage.getImplementationVersion().strip();
        }

        try (InputStream stream = SamanApplication.class.getResourceAsStream(
                "/META-INF/maven/com.car.rental/saman/pom.properties")) {
            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                String version = properties.getProperty("version");
                if (isUsable(version)) return version.strip();
            }
        } catch (IOException e) {
            log.log(Level.FINE, "Could not read Maven version metadata", e);
        }
        return null;
    }

    private static boolean isUsable(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value.strip());
    }
}
