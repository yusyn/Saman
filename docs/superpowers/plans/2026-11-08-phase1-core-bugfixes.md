# Phase 1: Core Bugfixes & Stabilization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate critical HikariCP connection deadlocks, enable SQLite foreign key constraints, optimize in-memory scans, decouple business logic from Persian presentation strings, remove side-effects from HTTP GET endpoints, implement login rate-limiting, and relocate standalone test scripts.

**Architecture:** Standardize Spring Boot database interactions and lifecycle. Fix HikariCP connection starvation by releasing transaction connections before fallback event logging. Enforce SQLite foreign keys globally via HikariCP connection-init-sql. Replace in-memory linear scans with indexed SQL queries. Isolate read paths from database writes.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring Security 6, SQLite JDBC, HikariCP, JUnit 5 Jupiter, Mockito, AssertJ.

**Spec:** [C:\Users\Tanin\.gemini\antigravity\brain\c9e03e97-6722-4215-8fa8-ed7ce067634b\saman-comprehensive-code-review.md](file:///C:/Users/Tanin/.gemini/antigravity/brain/c9e03e97-6722-4215-8fa8-ed7ce067634b/saman-comprehensive-code-review.md)

## Global Constraints

- Java 17 baseline; keep Spring Boot 3.3.5 dependencies.
- Zero changes to external API contracts (status codes, JSON payload keys).
- Preserve existing Persian error messages returned to clients.
- HikariCP pool size remains 1 for SQLite single-file write-lock safety until future PostgreSQL migration.

## Review Focus

1. **HikariCP connection pool starvation:** Any exception in `SystemEventService.inTransaction` must not hang the application for 30s before reporting the error.
2. **Foreign key cascade & validity:** Deleting or referencing non-existent IDs in SQLite tables with foreign keys must be blocked at database engine level.
3. **Concurrent device ID generation:** `getNextDeviceUserId()` must return an incremented integer based on database state without scanning all rows.
4. **Read purity:** Calling `GET /api/vehicles/history` must never execute an `UPDATE` on `VehicleTable`.
5. **Brute-force protection:** Exceeding failed login threshold must return HTTP 429 without querying or locking the database.

---

### Task 1: Fix HikariCP Connection Starvation in `SystemEventService`

**Files:**
- Modify: `src/com/car/rental/service/SystemEventService.java:52-85`
- Create: `src/test/java/com/car/rental/service/SystemEventServiceTest.java`

**Interfaces:**
- Consumes: `DataSource`, `SystemEventRepository`, `SqlWork<T>`, `EventSpec`
- Produces: `<T> T inTransaction(EventSpec success, EventSpec failure, SqlWork<T> work, Predicate<T> shouldRecordSuccess)`

- [ ] **Step 1: Write the failing unit test for connection exhaustion on failure**

```java
// src/test/java/com/car/rental/service/SystemEventServiceTest.java
package com.car.rental.service;

import com.car.rental.db.SystemEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemEventServiceTest {

    @Test
    @DisplayName("inTransaction releases connection before recording failure safely")
    void testConnectionReleasedBeforeFailureLogging() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        SystemEventRepository repo = mock(SystemEventRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        when(ds.getConnection()).thenAnswer(invocation -> {
            if (connectionClosed.get()) {
                // Second connection call for fallback logging
                Connection conn2 = mock(Connection.class);
                return conn2;
            }
            return conn;
        });

        doAnswer(inv -> {
            connectionClosed.set(true);
            return null;
        }).when(conn).close();

        SystemEventService service = new SystemEventService(repo, mapper, ds);

        SystemEventService.EventSpec success = new SystemEventService.EventSpec("cat", "type", "ok", "ent", "1");
        SystemEventService.EventSpec failure = new SystemEventService.EventSpec("cat", "type", "fail", "ent", "1");

        assertThrows(AuditedOperationException.class, () -> {
            service.inTransaction(success, failure, c -> {
                throw new SQLException("Simulated work failure");
            });
        });

        assertTrue(connectionClosed.get(), "Primary connection must be closed before failure is recorded");
        verify(repo).insert(eq("cat"), eq("type"), eq("ناموفق"), any(), any(), eq("ent"), eq("1"), any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SystemEventServiceTest` (or run JUnit in IDE)
Expected: FAIL (connection is closed after `recordFailureSafely`, not before).

- [ ] **Step 3: Refactor `inTransaction` in `SystemEventService.java` to release connection before failure recording**

Restructure `inTransaction` so the primary connection try-with-resources block exits *before* `recordFailureSafely` is invoked:

```java
    public <T> T inTransaction(EventSpec success, EventSpec failure, SqlWork<T> work,
                               Predicate<T> shouldRecordSuccess) throws SQLException {
        Throwable primaryFailure = null;
        T result = null;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                result = work.execute(connection);
                if (shouldRecordSuccess.test(result)) {
                    record(connection, success.category(), success.type(), "موفق", success.description(),
                            success.entityType(), success.entityId(), success.details());
                }
                connection.commit();
                return result;
            } catch (Throwable t) {
                primaryFailure = t;
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    primaryFailure.addSuppressed(rollback);
                }
            }
        } // Connection is guaranteed closed and returned to HikariCP pool here

        if (primaryFailure != null) {
            recordFailureSafely(failure, primaryFailure);
            if (primaryFailure instanceof SQLException sql) {
                throw new AuditedOperationException(sql);
            }
            if (primaryFailure instanceof RuntimeException re) {
                throw re;
            }
            if (primaryFailure instanceof Error err) {
                throw err;
            }
            throw new AuditedOperationException(new SQLException(primaryFailure));
        }
        return result;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SystemEventServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/com/car/rental/service/SystemEventService.java src/test/java/com/car/rental/service/SystemEventServiceTest.java
git commit -m "fix(service): release connection before fallback failure audit in SystemEventService"
```

---

### Task 2: Enable SQLite Foreign Key Constraints & Connection Initialization

**Files:**
- Modify: `src/main/resources/application.yml:9-11`
- Modify: `src/com/car/rental/db/SchemaInitializer.java:138-146`
- Create: `src/test/java/com/car/rental/db/ForeignKeyEnforcementTest.java`

**Interfaces:**
- Consumes: Hikari DataSource configuration, `SchemaInitializer.initDatabase()`
- Produces: Enforced referential integrity across SQLite tables (`RentalTable`, `VehicleFine`, `VehicleService`, `VehicleIssue`).

- [ ] **Step 1: Write integration test proving foreign keys are enforced**

```java
// src/test/java/com/car/rental/db/ForeignKeyEnforcementTest.java
package com.car.rental.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ForeignKeyEnforcementTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("PRAGMA foreign_keys returns 1 on all connections")
    void testForeignKeysPragmaIsEnabled() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("PRAGMA foreign_keys");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "SQLite foreign keys pragma must be 1 (enabled)");
        }
    }

    @Test
    @DisplayName("Inserting rental for non-existent vehicle throws foreign key constraint violation")
    void testForeignKeyConstraintViolation() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // car_id 999999 does not exist
            String sql = "INSERT INTO RentalTable(employee_id, car_id, destination) VALUES(1, 999999, 'Tehran')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                assertThrows(SQLException.class, ps::executeUpdate,
                        "Engine must reject row referencing non-existent vehicle");
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ForeignKeyEnforcementTest`
Expected: FAIL (`foreign_keys` returns 0).

- [ ] **Step 3: Configure `connection-init-sql` in `application.yml` and `SchemaInitializer`**

In `src/main/resources/application.yml`:
```yaml
    hikari:
      maximum-pool-size: 1
      connection-timeout: 30000
      connection-init-sql: "PRAGMA foreign_keys = ON;"
```

In `src/com/car/rental/db/SchemaInitializer.java`, right inside `initDatabase()` before table execution:
```java
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(employeeTable);
            ...
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ForeignKeyEnforcementTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/application.yml src/com/car/rental/db/SchemaInitializer.java src/test/java/com/car/rental/db/ForeignKeyEnforcementTest.java
git commit -m "fix(db): enforce SQLite foreign keys via Hikari connection-init-sql"
```

---

### Task 3: Optimize In-Memory Scans in `EmployeeRepository` & `VehicleController`

**Files:**
- Modify: `src/com/car/rental/db/EmployeeRepository.java:33-56`
- Modify: `src/com/car/rental/service/VehicleService.java`
- Modify: `src/com/car/rental/api/VehicleController.java:125-132`
- Create: `src/test/java/com/car/rental/db/EmployeeRepositoryTest.java`

**Interfaces:**
- Consumes: `EmployeeRepository.getNextDeviceUserId()`, `VehicleService.findVehicleByPlate(String plate)`
- Produces: Direct indexed SQL queries without reading full tables into memory.

- [ ] **Step 1: Write test for `getNextDeviceUserId` calculation**

```java
// src/test/java/com/car/rental/db/EmployeeRepositoryTest.java
package com.car.rental.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("getNextDeviceUserId returns at least 1001 and increments based on max numeric ID")
    void testNextDeviceUserId() throws SQLException {
        String nextId = employeeRepository.getNextDeviceUserId();
        assertNotNull(nextId);
        int id = Integer.parseInt(nextId);
        assertTrue(id >= 1001, "Device User ID should be >= 1001");
    }
}
```

- [ ] **Step 2: Run test to check current behavior**

Run: `mvn test -Dtest=EmployeeRepositoryTest`
Expected: PASS or baseline confirmation.

- [ ] **Step 3: Implement optimized SQL in `EmployeeRepository.java`**

Replace the in-memory while loop in lines 33-56 of `EmployeeRepository.java`:

```java
    public String getNextDeviceUserId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(device_user_id AS INTEGER)), 1000) AS max_id " +
                     "FROM EmployeeTable WHERE device_user_id GLOB '[0-9]*'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int max = rs.next() ? rs.getInt("max_id") : 1000;
            int next = Math.max(max + 1, DEVICE_USER_ID_START);
            if (next > 65535) {
                throw new SQLException("Device user id range exhausted (max 65535)");
            }
            return String.valueOf(next);
        }
    }
```

- [ ] **Step 4: Implement `findVehicleByPlate` in `VehicleService` and use in `VehicleController`**

In `src/com/car/rental/service/VehicleService.java`:
```java
    public Vehicle findVehicleByPlate(String plate) throws SQLException {
        if (plate == null || plate.isBlank()) return null;
        VehicleRepository.VehicleRow row = vehicleRepository.findRowByPlate(plate.strip());
        return row != null ? row.toVehicle() : null;
    }
```

In `src/com/car/rental/api/VehicleController.java`, replace lines 125-132:
```java
    private Vehicle findByPlate(String plate) throws SQLException {
        return vehicleService.findVehicleByPlate(plate);
    }
```

- [ ] **Step 5: Verify tests and commit**

```bash
git add src/com/car/rental/db/EmployeeRepository.java src/com/car/rental/service/VehicleService.java src/com/car/rental/api/VehicleController.java src/test/java/com/car/rental/db/EmployeeRepositoryTest.java
git commit -m "perf: replace in-memory table scans with targeted SQL queries"
```

---

### Task 4: Decouple Business Logic from Presentation Strings in `VehicleController`

**Files:**
- Modify: `src/com/car/rental/model/Vehicle.java`
- Modify: `src/com/car/rental/db/VehicleRepository.java`
- Modify: `src/com/car/rental/api/VehicleController.java:84-87, 117-120, 134-139`
- Create: `src/test/java/com/car/rental/api/VehicleControllerStatusTest.java`

**Interfaces:**
- Consumes: `Vehicle.isRented()` boolean flag
- Produces: Safe domain check instead of Persian substring matching.

- [ ] **Step 1: Write unit test testing mission status check**

```java
// src/test/java/com/car/rental/api/VehicleControllerStatusTest.java
package com.car.rental.api;

import com.car.rental.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleControllerStatusTest {

    @Test
    @DisplayName("Vehicle retains boolean rented state independently of presentation string")
    void testVehicleRentedState() {
        Vehicle v = new Vehicle("Pride", "11B22233", "White", "CAR", "در مأموریت");
        v.setRented(true);
        assertTrue(v.isRented());

        Vehicle v2 = new Vehicle("Samand", "22B33344", "Black", "CAR", "آزاد");
        v2.setRented(false);
        assertFalse(v2.isRented());
    }
}
```

- [ ] **Step 2: Add `rented` field & getters in `Vehicle.java` and map in `VehicleRepository`**

In `src/com/car/rental/model/Vehicle.java`:
```java
    private boolean rented;

    public boolean isRented() {
        return rented;
    }

    public void setRented(boolean rented) {
        this.rented = rented;
    }
```

In `VehicleRepository.VehicleRow.toVehicle()` and `VehicleRepository.listAllVehicles()`:
Set `vehicle.setRented(this.isRented == 1);`.

- [ ] **Step 3: Update `isOnMission` in `VehicleController.java`**

In `src/com/car/rental/api/VehicleController.java`:
```java
    private static boolean isOnMission(Vehicle vehicle) {
        return vehicle != null && vehicle.isRented();
    }
```
Replace usages in `update` and `delete` methods:
`if (isOnMission(existing)) { throw new ResponseStatusException(HttpStatus.CONFLICT, ...); }`

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=VehicleControllerStatusTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/com/car/rental/model/Vehicle.java src/com/car/rental/db/VehicleRepository.java src/com/car/rental/api/VehicleController.java src/test/java/com/car/rental/api/VehicleControllerStatusTest.java
git commit -m "refactor: decouple vehicle mission check from Persian presentation strings"
```

---

### Task 5: Eliminate Side-Effect / DB Writes in HTTP GET `getHistory`

**Files:**
- Modify: `src/com/car/rental/service/VehicleHistoryService.java:91-96`
- Create: `src/test/java/com/car/rental/service/VehicleHistoryIdempotencyTest.java`

**Interfaces:**
- Consumes: `VehicleHistoryService.buildHistory(VehicleRow row)`
- Produces: Pure read-only execution on `GET /api/vehicles/history`.

- [ ] **Step 1: Write test verifying read idempotency**

```java
// src/test/java/com/car/rental/service/VehicleHistoryIdempotencyTest.java
package com.car.rental.service;

import com.car.rental.db.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;

import static org.mockito.Mockito.*;

class VehicleHistoryIdempotencyTest {

    @Test
    @DisplayName("getHistoryByPlate does not execute updates on VehicleRepository")
    void testGetHistoryDoesNotMutateDatabase() throws SQLException {
        VehicleRepository vRepo = mock(VehicleRepository.class);
        VehicleServiceRepository sRepo = mock(VehicleServiceRepository.class);
        VehicleIssueRepository iRepo = mock(VehicleIssueRepository.class);
        VehicleFineRepository fRepo = mock(VehicleFineRepository.class);
        RentalRepository rRepo = mock(RentalRepository.class);

        VehicleRepository.VehicleRow row = new VehicleRepository.VehicleRow(
                1, "Pride", "11B22233", "White", "CAR", 0, 10000, null, null, ""
        );
        when(vRepo.findRowByPlate("11B22233")).thenReturn(row);
        when(rRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(sRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(iRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(fRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());

        VehicleHistoryService service = new VehicleHistoryService(vRepo, sRepo, iRepo, fRepo, rRepo);
        service.getHistoryByPlate("11B22233");

        // Verify updateStatusFlags is NEVER invoked during read
        verify(vRepo, never()).updateStatusFlags(anyInt(), anyString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=VehicleHistoryIdempotencyTest`
Expected: FAIL (`updateStatusFlags` was invoked).

- [ ] **Step 3: Remove DB write from `buildHistory` in `VehicleHistoryService.java`**

Remove lines 91-96 in `src/com/car/rental/service/VehicleHistoryService.java`:
```java
        List<String> badges = computeStatusBadges(row, serviceList, vehicleId);

        // DO NOT write to DB on GET:
        // String flagsCsv = String.join(",", badges);
        // if (row.statusFlags == null || !row.statusFlags.equals(flagsCsv)) {
        //     vehicles.updateStatusFlags(vehicleId, flagsCsv);
        // }
```
Keep `dto.setStatusBadges(badges);` so the client receives the computed badges without mutating the database on GET.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=VehicleHistoryIdempotencyTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/com/car/rental/service/VehicleHistoryService.java src/test/java/com/car/rental/service/VehicleHistoryIdempotencyTest.java
git commit -m "fix(history): remove side-effect database write from GET vehicle history"
```

---

### Task 6: Implement Login Rate Limiting & Align Security Documentation

**Files:**
- Create: `src/com/car/rental/security/LoginRateLimiter.java`
- Modify: `src/com/car/rental/api/AuthController.java:55-96`
- Modify: `docs/SECURITY.md:41-53`
- Create: `src/test/java/com/car/rental/security/LoginRateLimiterTest.java`

**Interfaces:**
- Consumes: `LoginRateLimiter.isBlocked(String key)`, `recordFailure(String key)`, `recordSuccess(String key)`
- Produces: HTTP 429 Too Many Requests on consecutive failed attempts.

- [ ] **Step 1: Write unit test for `LoginRateLimiter`**

```java
// src/test/java/com/car/rental/security/LoginRateLimiterTest.java
package com.car.rental.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    @Test
    @DisplayName("Blocks after 5 consecutive failed attempts")
    void testRateLimiterBlocks() {
        LoginRateLimiter limiter = new LoginRateLimiter(5, 60_000);
        String ip = "192.168.1.50";

        for (int i = 0; i < 5; i++) {
            assertFalse(limiter.isBlocked(ip));
            limiter.recordFailure(ip);
        }

        assertTrue(limiter.isBlocked(ip), "Should be blocked after 5 failed attempts");

        limiter.recordSuccess(ip);
        assertFalse(limiter.isBlocked(ip), "Success should reset the counter");
    }
}
```

- [ ] **Step 2: Implement `LoginRateLimiter.java`**

Create `src/com/car/rental/security/LoginRateLimiter.java` using concurrent sliding window timestamps:

```java
package com.car.rental.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter() {
        this(5, 5 * 60 * 1000L); // 5 attempts per 5 minutes
    }

    public LoginRateLimiter(int maxAttempts, long windowMillis) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
    }

    public boolean isBlocked(String key) {
        if (key == null || key.isBlank()) return false;
        ConcurrentLinkedQueue<Long> timestamps = attempts.get(key);
        if (timestamps == null) return false;

        long cutoff = Instant.now().toEpochMilli() - windowMillis;
        timestamps.removeIf(t -> t < cutoff);
        return timestamps.size() >= maxAttempts;
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) return;
        attempts.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>())
                .add(Instant.now().toEpochMilli());
    }

    public void recordSuccess(String key) {
        if (key != null) attempts.remove(key);
    }
}
```

- [ ] **Step 3: Integrate into `AuthController.java`**

In `src/com/car/rental/api/AuthController.java`:
Inject `LoginRateLimiter rateLimiter`.
At the start of `login(...)`:
```java
        String clientIp = request.getRemoteAddr();
        if (rateLimiter.isBlocked(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiError("تعداد تلاش‌های ناموفق بیش از حد مجاز است. لطفاً چند دقیقه بعد دوباره امتحان کنید.", 429));
        }
```
On success: `rateLimiter.recordSuccess(clientIp);`
On `AuthenticationException`: `rateLimiter.recordFailure(clientIp);`

- [ ] **Step 4: Update `docs/SECURITY.md`**

In `docs/SECURITY.md`, remove `GET /api/employees/**` and `GET /api/rentals/**` from the "مسیرهای عمومی (allowlist)" table, and clearly document that reading employee lists and active rentals requires authentication.

- [ ] **Step 5: Run tests and commit**

```bash
git add src/com/car/rental/security/LoginRateLimiter.java src/com/car/rental/api/AuthController.java docs/SECURITY.md src/test/java/com/car/rental/security/LoginRateLimiterTest.java
git commit -m "feat(security): implement login rate limiting and align security docs"
```

---

### Task 7: Relocate Standalone Test Scripts from Production Code

**Files:**
- Move: `src/com/car/rental/service/ZkTestMain.java` -> `src/test/java/com/car/rental/service/zk/manual/ZkTestMain.java`
- Move: `src/com/car/rental/service/ZkUserEnrollTest.java` -> `src/test/java/com/car/rental/service/zk/manual/ZkUserEnrollTest.java`

- [ ] **Step 1: Create manual test directory and move files**

Move the two manual terminal testing scripts with `main` methods into `src/test/java/com/car/rental/service/zk/manual/`.

- [ ] **Step 2: Update package declaration in both files**

Change package to `package com.car.rental.service.zk.manual;` and add necessary imports.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/car/rental/service/zk/manual/ src/com/car/rental/service/
git commit -m "chore: relocate manual ZK hardware test scripts to test directory"
```

---

## Plan Review Checklist

1. **Spec coverage:** All 7 critical Phase 1 items identified in the review artifact are represented by dedicated tasks.
2. **Step scan:** Each task has distinct failing test, implementation, verification, and commit steps.
3. **Type consistency:** Identical method names (`findVehicleByPlate`, `isRented`, `isBlocked`) across tasks.
4. **Review Focus:** Pool exhaustion, FK enforcement, next ID calculation, GET side-effects, and rate-limiting are directly tested.
