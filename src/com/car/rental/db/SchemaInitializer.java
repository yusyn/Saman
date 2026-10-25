package com.car.rental.db;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
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
@Order(1)
public class SchemaInitializer implements ApplicationRunner {

    private static final Logger logger = Logger.getLogger(SchemaInitializer.class.getName());

    private final DataSource dataSource;

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        initDatabase();
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

            String appUserTable =
                    "CREATE TABLE IF NOT EXISTS AppUser (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password_hash TEXT NOT NULL, " +
                    "role TEXT NOT NULL DEFAULT 'ADMIN', " +
                    "enabled INTEGER NOT NULL DEFAULT 1, " +
                    "created_at TEXT DEFAULT (datetime('now','localtime'))" +
                    ")";
            stmt.execute(appUserTable);

            stmt.execute("CREATE TABLE IF NOT EXISTS SystemEvent (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "occurred_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
                    "category TEXT NOT NULL, event_type TEXT NOT NULL, result TEXT NOT NULL " +
                    "CHECK(result IN ('موفق','ناموفق')), description TEXT NOT NULL, " +
                    "actor TEXT, entity_type TEXT, entity_id TEXT, details_json TEXT)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_system_event_time ON SystemEvent(occurred_at DESC, id DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_system_event_category_type ON SystemEvent(category, event_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_system_event_result ON SystemEvent(result)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_system_event_actor ON SystemEvent(actor)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_system_event_entity ON SystemEvent(entity_type, entity_id)");
            stmt.execute("CREATE TABLE IF NOT EXISTS AppUserAuditContext (" +
                    "id INTEGER PRIMARY KEY CHECK(id=1), actor TEXT)");
            stmt.execute("INSERT OR IGNORE INTO AppUserAuditContext(id,actor) VALUES(1,NULL)");
            stmt.execute("UPDATE AppUserAuditContext SET actor=NULL WHERE id=1");
            initializeAppUserAuditTriggers(stmt);
            stmt.execute("CREATE TABLE IF NOT EXISTS SystemSnapshot (" +
                    "snapshot_key TEXT PRIMARY KEY, snapshot_value TEXT NOT NULL, " +
                    "updated_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))) ");
            stmt.execute("CREATE TABLE IF NOT EXISTS DeviceOperation (" +
                    "operation_id TEXT PRIMARY KEY, operation_type TEXT NOT NULL, entity_type TEXT NOT NULL, entity_id TEXT, " +
                    "actor TEXT, state TEXT NOT NULL CHECK(state IN ('PENDING','DEVICE_SUCCEEDED','DB_COMMITTED','COMPLETED','FAILED','RECONCILIATION_REQUIRED')), " +
                    "device_succeeded INTEGER NOT NULL DEFAULT -1, note TEXT, " +
                    "details_json TEXT NOT NULL DEFAULT '{}', " +
                    "created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), " +
                    "updated_at TEXT NOT NULL DEFAULT (datetime('now','localtime')))");
            ensureColumn(conn, "DeviceOperation", "details_json", "ALTER TABLE DeviceOperation ADD COLUMN details_json TEXT NOT NULL DEFAULT '{}'");
            stmt.execute("UPDATE DeviceOperation SET device_succeeded=-1 WHERE state='PENDING' AND device_succeeded=0");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_device_operation_state ON DeviceOperation(state, updated_at DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_device_operation_entity ON DeviceOperation(entity_type, entity_id, state)");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_device_operation_open_entity ON DeviceOperation(entity_type, entity_id) WHERE state NOT IN ('COMPLETED','FAILED')");

            migrateCarTableToVehicleTable(conn);
            ensureVehicleTypeColumn(conn);
            ensureVehicleHistoryColumns(conn);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database init failed!", e);
        }
    }

    /**
     * SQLite triggers are the sole producers of AppUser audit events. Repository writes only set
     * transaction-local attribution; direct SQL writes therefore fall back to an explicit unknown actor.
     */
    private void initializeAppUserAuditTriggers(Statement stmt) throws SQLException {
        String actor = "COALESCE(NULLIF((SELECT actor FROM AppUserAuditContext WHERE id=1),''),'نامشخص/خارج از برنامه')";
        stmt.execute("CREATE TRIGGER IF NOT EXISTS trg_app_user_audit_insert AFTER INSERT ON AppUser BEGIN " +
                "INSERT INTO SystemEvent(category,event_type,result,description,actor,entity_type,entity_id,details_json) VALUES(" +
                "'حساب‌های مدیر','ایجاد حساب مدیر','موفق','حساب مدیر ایجاد شد؛ زمان ثبت زمان اجرای تریگر SQLite است'," + actor + ",'حساب مدیر',CAST(NEW.id AS TEXT),'{}'); END");
        stmt.execute("CREATE TRIGGER IF NOT EXISTS trg_app_user_audit_update AFTER UPDATE ON AppUser " +
                "WHEN OLD.username IS NOT NEW.username OR OLD.password_hash IS NOT NEW.password_hash " +
                "OR OLD.role IS NOT NEW.role OR OLD.enabled IS NOT NEW.enabled BEGIN " +
                "INSERT INTO SystemEvent(category,event_type,result,description,actor,entity_type,entity_id,details_json) VALUES(" +
                "'حساب‌های مدیر',CASE WHEN OLD.password_hash IS NOT NEW.password_hash " +
                "AND OLD.username IS NEW.username AND OLD.role IS NEW.role AND OLD.enabled IS NEW.enabled " +
                "THEN 'تغییر رمز عبور مدیر' ELSE 'ویرایش حساب مدیر' END,'موفق'," +
                "CASE WHEN OLD.password_hash IS NOT NEW.password_hash AND OLD.username IS NEW.username " +
                "AND OLD.role IS NEW.role AND OLD.enabled IS NEW.enabled " +
                "THEN 'رمز عبور حساب مدیر تغییر کرد؛ زمان ثبت زمان اجرای تریگر SQLite است' " +
                "ELSE 'مشخصات حساب مدیر تغییر کرد؛ زمان ثبت زمان اجرای تریگر SQLite است' END," + actor + ",'حساب مدیر',CAST(NEW.id AS TEXT)," +
                "json_object('source','sqlite_trigger','changedFields',json('[' || rtrim(" +
                "CASE WHEN OLD.username IS NOT NEW.username THEN '\"username\",' ELSE '' END || " +
                "CASE WHEN OLD.role IS NOT NEW.role THEN '\"role\",' ELSE '' END || " +
                "CASE WHEN OLD.enabled IS NOT NEW.enabled THEN '\"enabled\",' ELSE '' END || " +
                "CASE WHEN OLD.password_hash IS NOT NEW.password_hash THEN '\"password\",' ELSE '' END, ',') || ']')," +
                "'old',json_object('username',CASE WHEN OLD.username IS NOT NEW.username THEN OLD.username END," +
                "'role',CASE WHEN OLD.role IS NOT NEW.role THEN OLD.role END," +
                "'enabled',CASE WHEN OLD.enabled IS NOT NEW.enabled THEN OLD.enabled END)," +
                "'new',json_object('username',CASE WHEN OLD.username IS NOT NEW.username THEN NEW.username END," +
                "'role',CASE WHEN OLD.role IS NOT NEW.role THEN NEW.role END," +
                "'enabled',CASE WHEN OLD.enabled IS NOT NEW.enabled THEN NEW.enabled END))); END");
        stmt.execute("CREATE TRIGGER IF NOT EXISTS trg_app_user_audit_delete AFTER DELETE ON AppUser BEGIN " +
                "INSERT INTO SystemEvent(category,event_type,result,description,actor,entity_type,entity_id,details_json) VALUES(" +
                "'حساب‌های مدیر','حذف حساب مدیر','موفق','حساب مدیر حذف شد؛ زمان ثبت زمان اجرای تریگر SQLite است'," + actor + ",'حساب مدیر',CAST(OLD.id AS TEXT),'{}'); END");
    }

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

    private void ensureVehicleTypeColumn(Connection conn) throws SQLException {
        ensureColumn(conn, "VehicleTable", "vehicle_type",
                "ALTER TABLE VehicleTable ADD COLUMN vehicle_type TEXT NOT NULL DEFAULT 'CAR'");
    }

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
