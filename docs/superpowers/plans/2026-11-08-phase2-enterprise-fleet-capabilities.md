# Phase 2: Enterprise Fleet Capabilities & Architecture Modernization Implementation Plan (Software-Only Edition)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Saman into a high-precision, low-friction fleet management system tailored specifically for the **Software-Only Edition** (freemium/cost-effective tier without OBD-II/ECU hardware). Key capabilities include:
1. Streamlined trip mileage tracking with single-point odometer input upon vehicle return (automatic starting mileage snapshotting and progression validation).
2. Complete omission of fuel tracking (avoiding low-fidelity manual fuel estimation without hardware sensors).
3. Insurance and technical inspection compliance monitoring with automated warning badges.
4. Declarative Jakarta Bean Validation across all REST controllers with centralized Persian error messaging.
5. Interactive OpenAPI/Swagger documentation for robust client integration.

**Architecture:** Maintain backward compatibility while evolving schema through additive non-breaking SQLite migrations. Extend `RentalTable` (odometer, distance, delivery/return notes) and `VehicleTable` (compliance expiry dates, policy details). Integrate `spring-boot-starter-validation` across all REST API controllers with centralized Persian error translation in `ApiExceptionHandler`. Add `springdoc-openapi-starter-webmvc-ui` for living interactive API documentation. Update the lightweight vanilla frontend with return odometer capture, compliance badges, and trip distance metrics.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring Security 6, Jakarta Bean Validation (Hibernate Validator), Springdoc OpenAPI 2.6.0, SQLite JDBC, HikariCP, HTML5/CSS3/Vanilla JS.

**Spec Reference:** [saman-comprehensive-code-review.md](file:///C:/Users/Tanin/.gemini/antigravity/brain/c9e03e97-6722-4215-8fa8-ed7ce067634b/saman-comprehensive-code-review.md)

---

## Strategic Product Alignment: Software-Only vs. Hardware Edition

| Feature | Software-Only Edition (Current Plan) | Hardware Edition (Future Tier with ECU/OBD-II) |
| :--- | :--- | :--- |
| **Hardware Dependency** | Zero sensors. Only ZKTeco biometric gate & web UI. | OBD-II dongle, GPS tracker, CAN-bus integration. |
| **Fuel Tracking** | **Completely Omitted.** Manual fuel estimation without sensors causes friction and unreliable data. | **Automated.** Live fuel percentage and consumption read directly from vehicle ECU. |
| **Odometer Capture** | **Return-Only.** Driver does not have the key at pickup; start mileage is auto-snapshotted from `VehicleTable.current_odometer`. Driver enters odometer only when returning the key. | **Automated.** Real-time odometer continuously streamed via OBD-II telematics. |
| **Compliance Monitoring** | Active (Third-party insurance, body insurance, technical inspection with automated status badges). | Active + automated DTC diagnostic engine fault alerts. |

---

## Global Constraints

- Java 17 baseline; keep Spring Boot 3.3.5 compatibility.
- Database migrations must be idempotent (`PRAGMA table_info` and `ALTER TABLE ADD COLUMN` pattern already established in `SchemaInitializer.java`).
- Zero breakage of existing API consumers; all new request fields must be optional or provide sensible defaults.
- All Persian messages and Iranian national plate formatting standards must be preserved and strictly validated.
- HikariCP pool size remains 1 with `PRAGMA foreign_keys = ON;`.

## Review Focus

1. **Odometer progression sanity:** `return_odometer` must be strictly $\ge$ `pickup_odometer` (where `pickup_odometer` = vehicle's `current_odometer` at start). Rollbacks or typos must be rejected with 400 Bad Request.
2. **Current vehicle odometer auto-update:** On vehicle return, `VehicleTable.current_odometer` must be automatically synchronized with `return_odometer` in the same transaction.
3. **Validation error format:** Bean Validation failures (`MethodArgumentNotValidException`) must return clean Persian field error descriptions in `ApiError` rather than dumping Java exception stack traces.
4. **Compliance expiry calculation:** Vehicle insurance and technical inspection statuses must compute accurate warning flags (`INSURANCE_EXPIRING`, `INSURANCE_EXPIRED`, `INSPECTION_EXPIRED`) based on the current date.
5. **Swagger UI security accessibility:** `/swagger-ui/**` and `/v3/api-docs/**` must be accessible in `SecurityConfig.java` without breaking session-based API protection.

---

### Task 1: Add Jakarta Validation & Springdoc OpenAPI Dependencies

**Files:**
- Modify: `pom.xml`
- Create: `src/com/car/rental/config/OpenApiConfig.java`
- Modify: `src/com/car/rental/security/SecurityConfig.java`
- Create: `src/test/java/com/car/rental/config/OpenApiIntegrationTest.java`

**Interfaces:**
- Consumes: Maven dependencies (`spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`)
- Produces: Live `/swagger-ui.html` and bean validation support on `@Valid` controller arguments.

- [ ] **Step 1: Write integration test verifying OpenAPI endpoints are available**

```java
// src/test/java/com/car/rental/config/OpenApiIntegrationTest.java
package com.car.rental.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OpenAPI JSON docs are publicly accessible")
    void testApiDocsAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Swagger UI redirects or loads successfully")
    void testSwaggerUiAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=OpenApiIntegrationTest`
Expected: FAIL (missing dependency & configuration).

- [ ] **Step 3: Update `pom.xml` with Validation & Springdoc OpenAPI**

Add to `pom.xml` under `<dependencies>`:
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.6.0</version>
        </dependency>
```

- [ ] **Step 4: Create `OpenApiConfig.java` & update `SecurityConfig.java`**

Create `src/com/car/rental/config/OpenApiConfig.java`:
```java
package com.car.rental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("سامانه مدیریت ناوگان سامان (Saman Fleet Management API)")
                        .version("1.0.0")
                        .description("مستندات تعاملی REST API سامانه سازمانی مدیریت ناوگان خودرویی و مأموریت‌ها - نسخه نرم‌افزاری")
                        .contact(new Contact().name("تیم توسعه سامان")));
    }
}
```

In `src/com/car/rental/security/SecurityConfig.java`, allow OpenAPI and Swagger UI paths:
```java
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.svg",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/icons/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=OpenApiIntegrationTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/com/car/rental/config/OpenApiConfig.java src/com/car/rental/security/SecurityConfig.java src/test/java/com/car/rental/config/OpenApiIntegrationTest.java
git commit -m "feat(api): add validation and Springdoc OpenAPI documentation"
```

---

### Task 2: Database Schema Evolution for Mileage, Notes & Compliance (No Fuel)

**Files:**
- Modify: `src/com/car/rental/db/SchemaInitializer.java`
- Create: `src/test/java/com/car/rental/db/Phase2SchemaMigrationTest.java`

**Interfaces:**
- Consumes: SQLite DataSource
- Produces: Extended `RentalTable` (columns: `pickup_odometer`, `return_odometer`, `distance_km`, `inspection_notes`, `return_notes`) and `VehicleTable` (columns: `insurance_expiry_date`, `body_insurance_expiry_date`, `inspection_expiry_date`, `insurance_company`, `insurance_policy_number`).
- Note: Zero fuel columns (`pickup_fuel_level` and `return_fuel_level` are eliminated).

- [ ] **Step 1: Write integration test verifying columns exist after startup**

```java
// src/test/java/com/car/rental/db/Phase2SchemaMigrationTest.java
package com.car.rental.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class Phase2SchemaMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("RentalTable contains mileage and notes columns but no fuel columns")
    void testRentalTableColumns() throws SQLException {
        Set<String> columns = getColumns("RentalTable");
        assertTrue(columns.contains("pickup_odometer"), "pickup_odometer must exist");
        assertTrue(columns.contains("return_odometer"), "return_odometer must exist");
        assertTrue(columns.contains("distance_km"), "distance_km must exist");
        assertTrue(columns.contains("inspection_notes"), "inspection_notes must exist");
        assertTrue(columns.contains("return_notes"), "return_notes must exist");

        // Verify fuel fields are omitted in Software-Only edition
        assertFalse(columns.contains("pickup_fuel_level"), "pickup_fuel_level must NOT exist in software-only edition");
        assertFalse(columns.contains("return_fuel_level"), "return_fuel_level must NOT exist in software-only edition");
    }

    @Test
    @DisplayName("VehicleTable contains insurance and compliance columns")
    void testVehicleTableComplianceColumns() throws SQLException {
        Set<String> columns = getColumns("VehicleTable");
        assertTrue(columns.contains("insurance_expiry_date"), "insurance_expiry_date must exist");
        assertTrue(columns.contains("body_insurance_expiry_date"), "body_insurance_expiry_date must exist");
        assertTrue(columns.contains("inspection_expiry_date"), "inspection_expiry_date must exist");
        assertTrue(columns.contains("insurance_company"), "insurance_company must exist");
        assertTrue(columns.contains("insurance_policy_number"), "insurance_policy_number must exist");
    }

    private Set<String> getColumns(String table) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                cols.add(rs.getString("name").toLowerCase());
            }
        }
        return cols;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=Phase2SchemaMigrationTest`
Expected: FAIL (columns do not exist yet).

- [ ] **Step 3: Update `SchemaInitializer.java` with migration methods**

In `src/com/car/rental/db/SchemaInitializer.java`:
1. Update `rentalTable` initial DDL with new columns (for new installations).
2. Add migration helper methods for existing databases:
   - `ensureRentalTripColumns(Connection conn)`
   - `ensureVehicleComplianceColumns(Connection conn)`
3. Call both in `initDatabase()`:

```java
    private void ensureRentalTripColumns(Connection conn) throws SQLException {
        ensureColumn(conn, "RentalTable", "pickup_odometer", "ALTER TABLE RentalTable ADD COLUMN pickup_odometer INTEGER");
        ensureColumn(conn, "RentalTable", "return_odometer", "ALTER TABLE RentalTable ADD COLUMN return_odometer INTEGER");
        ensureColumn(conn, "RentalTable", "distance_km", "ALTER TABLE RentalTable ADD COLUMN distance_km INTEGER");
        ensureColumn(conn, "RentalTable", "inspection_notes", "ALTER TABLE RentalTable ADD COLUMN inspection_notes TEXT");
        ensureColumn(conn, "RentalTable", "return_notes", "ALTER TABLE RentalTable ADD COLUMN return_notes TEXT");
    }

    private void ensureVehicleComplianceColumns(Connection conn) throws SQLException {
        ensureColumn(conn, "VehicleTable", "insurance_expiry_date", "ALTER TABLE VehicleTable ADD COLUMN insurance_expiry_date TEXT");
        ensureColumn(conn, "VehicleTable", "body_insurance_expiry_date", "ALTER TABLE VehicleTable ADD COLUMN body_insurance_expiry_date TEXT");
        ensureColumn(conn, "VehicleTable", "inspection_expiry_date", "ALTER TABLE VehicleTable ADD COLUMN inspection_expiry_date TEXT");
        ensureColumn(conn, "VehicleTable", "insurance_company", "ALTER TABLE VehicleTable ADD COLUMN insurance_company TEXT");
        ensureColumn(conn, "VehicleTable", "insurance_policy_number", "ALTER TABLE VehicleTable ADD COLUMN insurance_policy_number TEXT");
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=Phase2SchemaMigrationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/com/car/rental/db/SchemaInitializer.java src/test/java/com/car/rental/db/Phase2SchemaMigrationTest.java
git commit -m "feat(db): add schema migrations for trip mileage, return notes and compliance"
```

---

### Task 3: Declarative Jakarta Bean Validation & Global Error Handler

**Files:**
- Modify: `src/com/car/rental/api/dto/PickupRequest.java`
- Modify: `src/com/car/rental/api/dto/ReturnRequest.java`
- Modify: `src/com/car/rental/api/dto/CreateVehicleRequest.java`
- Modify: `src/com/car/rental/api/dto/RegisterEmployeeRequest.java`
- Modify: `src/com/car/rental/api/ApiExceptionHandler.java`
- Modify: `src/com/car/rental/api/RentalController.java`
- Modify: `src/com/car/rental/api/VehicleController.java`
- Modify: `src/com/car/rental/api/EmployeeController.java`
- Create: `src/test/java/com/car/rental/api/ValidationExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `@Valid`, `@NotBlank`, `@Min`, `@Pattern`, `@Size`
- Produces: Standardized Persian JSON `ApiError` with HTTP 400 on validation failure.

- [ ] **Step 1: Write controller test for Bean Validation errors**

```java
// src/test/java/com/car/rental/api/ValidationExceptionHandlerTest.java
package com.car.rental.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ValidationExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Invalid vehicle creation payload returns 400 with Persian field error")
    void testInvalidVehiclePayload() throws Exception {
        String invalidJson = """
            {"name": "", "plate": "", "color": ""}
        """;

        mockMvc.perform(post("/api/vehicles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("الزامی")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Return payload with negative odometer returns 400 with Persian field error")
    void testNegativeReturnOdometerValidation() throws Exception {
        String invalidJson = """
            {"deviceUserId": "1", "returnOdometer": -10}
        """;

        mockMvc.perform(post("/api/rentals/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("منفی")));
    }
}
```

- [ ] **Step 2: Add validation annotations to DTOs**

1. In `CreateVehicleRequest.java`:
   - `@NotBlank(message = "نام و مدل وسیله الزامی است")`
   - `@NotBlank(message = "پلاک وسیله الزامی است")`
   - `@NotBlank(message = "رنگ وسیله الزامی است")`
   - `@Min(value = 0, message = "کیلومتر اولیه نمی‌تواند منفی باشد")` for `initialOdometer` (optional, defaults to 0).
2. In `PickupRequest.java`:
   - `@NotBlank(message = "شناسه کاربر الزامی است")`
   - `@NotBlank(message = "پلاک وسیله الزامی است")`
   - `@NotBlank(message = "مقصد الزامی است")`
   - (Note: No `pickupOdometer`! Driver does not input odometer at pickup).
3. In `ReturnRequest.java`:
   - `@NotBlank(message = "شناسه کاربر الزامی است")`
   - `@NotNull(message = "کیلومتر بازگشت الزامی است")`
   - `@Min(value = 0, message = "کیلومتر بازگشت نمی‌تواند منفی باشد")`
4. In `RegisterEmployeeRequest.java`:
   - `@NotBlank(message = "نام کامل انگلیسی الزامی است")`
   - `@Pattern(regexp = "^09\\d{9}$", message = "شماره تماس باید ۱۱ رقم با پیش‌شماره 09 باشد")` (if present).

- [ ] **Step 3: Update `ApiExceptionHandler.java` to handle `MethodArgumentNotValidException`**

```java
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("ورودی نامعتبر است");
        systemEvents.recordFailureSafely(new SystemEventService.EventSpec("API", "خطای اعتبارسنجی ورودی", message, "API", null), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message, 400));
    }
```

- [ ] **Step 4: Add `@Valid` to controller endpoints**

In `VehicleController`, `RentalController`, `EmployeeController`, annotate `@RequestBody @Valid ...`.

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=ValidationExceptionHandlerTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/com/car/rental/api/ src/com/car/rental/api/dto/ src/test/java/com/car/rental/api/ValidationExceptionHandlerTest.java
git commit -m "feat(validation): implement declarative bean validation and Persian error handling"
```

---

### Task 4: Streamlined Return-Only Odometer Progression & Synchronization in `RentalService`

**Files:**
- Modify: `src/com/car/rental/model/RentalRecord.java`
- Modify: `src/com/car/rental/api/dto/PickupRequest.java`
- Modify: `src/com/car/rental/api/dto/ReturnRequest.java`
- Modify: `src/com/car/rental/api/dto/RentalRecordDto.java`
- Modify: `src/com/car/rental/db/RentalRepository.java`
- Modify: `src/com/car/rental/service/RentalService.java`
- Create: `src/test/java/com/car/rental/service/RentalTripMileageTest.java`

**Interfaces:**
- Consumes: `pickupCar(deviceUserId, plate, destination, notes)` (starting odometer snapshotted internally from `VehicleTable.current_odometer`)
- Consumes: `returnCar(deviceUserId, returnOdometer, returnNotes)`
- Validates: `returnOdometer >= activeRental.pickupOdometer`. If less, rejects with `IllegalArgumentException` ("کیلومتر ثبت‌شده بازگشت (%d) نمی‌تواند کمتر از کیلومتر شروع (%d) باشد").
- Produces: Atomic database update: `RentalTable` (`return_odometer`, `distance_km`, `return_notes`) + `VehicleTable.current_odometer = returnOdometer`.

- [ ] **Step 1: Write unit and integration test for return odometer progression and sync**

```java
// src/test/java/com/car/rental/service/RentalTripMileageTest.java
package com.car.rental.service;

import com.car.rental.db.RentalRepository;
import com.car.rental.db.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RentalTripMileageTest {

    @Autowired
    private RentalService rentalService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    @DisplayName("Return odometer less than start odometer is rejected with IllegalArgumentException")
    void testReturnOdometerLessThanPickup() {
        assertThrows(IllegalArgumentException.class, () -> {
            rentalService.validateOdometerProgression(50000, 49900);
        });
    }

    @Test
    @DisplayName("Trip distance is correctly computed as return odometer minus start odometer")
    void testDistanceCalculation() {
        int distance = rentalService.calculateTripDistance(50000, 50250);
        assertEquals(250, distance, "Distance should be exactly 250 km");
    }

    @Test
    @DisplayName("Zero distance trip is valid (returned immediately without driving)")
    void testZeroDistanceTrip() {
        int distance = rentalService.calculateTripDistance(50000, 50000);
        assertEquals(0, distance);
    }
}
```

- [ ] **Step 2: Update `RentalRecord.java` and `RentalRepository.java`**

1. In `RentalRecord.java`:
   - `public Integer pickupOdometer;`
   - `public Integer returnOdometer;`
   - `public Integer distanceKm;`
   - `public String inspectionNotes;`
   - `public String returnNotes;`
2. In `RentalRepository.insertRental`:
   - Fetch vehicle's `current_odometer` (or pass it in).
   - Insert into `RentalTable (..., pickup_odometer, inspection_notes)`.
3. In `RentalRepository.returnCarByDeviceUserId`:
   - Read active rental's `pickup_odometer` and `vehicle_id`.
   - Validate `returnOdometer >= pickup_odometer`.
   - Calculate `distance_km = returnOdometer - pickup_odometer`.
   - Update `RentalTable SET return_date=?, return_odometer=?, distance_km=?, return_notes=?, is_active=0 WHERE id=?`.
   - Update `VehicleTable SET is_rented=0, current_odometer=? WHERE id=?`.

- [ ] **Step 3: Update `RentalService.java` with validation & mileage orchestration**

In `RentalService.java`:
1. Implement `validateOdometerProgression(int startOdo, int returnOdo)`.
2. Implement `calculateTripDistance(int startOdo, int returnOdo)`.
3. In `pickupCar`: auto-snapshot current odometer from `vehicleRepository.findByPlate(plate).getCurrentOdometer()`.
4. In `returnCar`: perform validation, calculate distance, synchronize vehicle odometer, and record audit event containing distance driven.

- [ ] **Step 4: Update `RentalController.java` & DTOs**

1. `PickupRequest`: accepts optional `notes`.
2. `ReturnRequest`: accepts `@NotNull @Min(0) Integer returnOdometer` and optional `returnNotes`.
3. `RentalRecordDto`: serializes `pickupOdometer`, `returnOdometer`, `distanceKm`, `returnNotes`.

- [ ] **Step 5: Run tests and verify**

Run: `mvn test -Dtest=RentalTripMileageTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/com/car/rental/model/RentalRecord.java src/com/car/rental/api/dto/ src/com/car/rental/db/RentalRepository.java src/com/car/rental/service/RentalService.java src/com/car/rental/api/RentalController.java src/test/java/com/car/rental/service/RentalTripMileageTest.java
git commit -m "feat(rentals): implement return odometer capture, progression validation and vehicle sync"
```

---

### Task 5: Vehicle Insurance & Technical Inspection Compliance Module

**Files:**
- Modify: `src/com/car/rental/model/Vehicle.java`
- Modify: `src/com/car/rental/db/VehicleRepository.java`
- Modify: `src/com/car/rental/service/VehicleHistoryService.java`
- Modify: `src/com/car/rental/api/dto/VehicleDto.java`
- Modify: `src/com/car/rental/api/dto/VehicleHistoryDto.java`
- Create: `src/com/car/rental/api/dto/UpdateVehicleComplianceRequest.java`
- Modify: `src/com/car/rental/api/VehicleController.java`
- Create: `src/test/java/com/car/rental/service/VehicleComplianceServiceTest.java`

**Interfaces:**
- Consumes: Insurance and technical inspection expiry dates
- Produces: Compliance status badges (`INSURANCE_EXPIRING`, `INSURANCE_EXPIRED`, `INSPECTION_EXPIRED`), updating `PUT /api/vehicles/{plate}/compliance`.

- [ ] **Step 1: Write unit test for compliance badge evaluation**

```java
// src/test/java/com/car/rental/service/VehicleComplianceServiceTest.java
package com.car.rental.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleComplianceServiceTest {

    @Test
    @DisplayName("Expired third-party insurance produces BADGE_INSURANCE_EXPIRED")
    void testExpiredInsuranceBadge() {
        VehicleHistoryService service = new VehicleHistoryService(null, null, null, null, null);
        String pastDate = LocalDate.now().minusDays(5).toString();
        List<String> badges = service.evaluateComplianceBadges(pastDate, null, null);
        assertTrue(badges.contains("INSURANCE_EXPIRED"));
    }

    @Test
    @DisplayName("Insurance expiring within 30 days produces BADGE_INSURANCE_EXPIRING")
    void testExpiringInsuranceBadge() {
        VehicleHistoryService service = new VehicleHistoryService(null, null, null, null, null);
        String soonDate = LocalDate.now().plusDays(15).toString();
        List<String> badges = service.evaluateComplianceBadges(soonDate, null, null);
        assertTrue(badges.contains("INSURANCE_EXPIRING"));
    }

    @Test
    @DisplayName("Expired technical inspection produces BADGE_INSPECTION_EXPIRED")
    void testExpiredInspectionBadge() {
        VehicleHistoryService service = new VehicleHistoryService(null, null, null, null, null);
        String pastDate = LocalDate.now().minusDays(1).toString();
        List<String> badges = service.evaluateComplianceBadges(null, null, pastDate);
        assertTrue(badges.contains("INSPECTION_EXPIRED"));
    }
}
```

- [ ] **Step 2: Add compliance fields to `Vehicle` and `VehicleRepository`**

1. In `Vehicle.java` & `VehicleRepository.VehicleRow`:
   - `insuranceExpiryDate`, `bodyInsuranceExpiryDate`, `inspectionExpiryDate`, `insuranceCompany`, `insurancePolicyNumber`.
2. In `VehicleRepository.java`:
   - Add `updateCompliance(int vehicleId, String insuranceExpiry, String bodyInsuranceExpiry, String inspectionExpiry, String company, String policyNo)`.

- [ ] **Step 3: Add compliance badge rules to `VehicleHistoryService.computeStatusBadges`**

Add badge definitions:
- `BADGE_INSURANCE_EXPIRED = "INSURANCE_EXPIRED"`
- `BADGE_INSURANCE_EXPIRING = "INSURANCE_EXPIRING"`
- `BADGE_INSPECTION_EXPIRED = "INSPECTION_EXPIRED"`
Implement `evaluateComplianceBadges` parsing ISO dates (`yyyy-MM-dd`) and checking against `LocalDate.now()`.

- [ ] **Step 4: Expose endpoint `PUT /api/vehicles/{plate}/compliance` in `VehicleController`**

Allow administrators to update insurance policy numbers and expiry dates.

- [ ] **Step 5: Run tests and verify**

Run: `mvn test -Dtest=VehicleComplianceServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/com/car/rental/model/Vehicle.java src/com/car/rental/db/VehicleRepository.java src/com/car/rental/service/VehicleHistoryService.java src/com/car/rental/api/ src/test/java/com/car/rental/service/VehicleComplianceServiceTest.java
git commit -m "feat(compliance): add vehicle insurance and inspection monitoring with automated status badges"
```

---

### Task 6: UI Enhancements for Software-Only Edition (Return Odometer & Compliance)

**Files:**
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/css/app.css`
- Modify: `src/main/resources/static/css/vehicle-history.css`
- Modify: `src/main/resources/static/js/app.js`
- Modify: `src/main/resources/static/js/api.js`
- Modify: `src/main/resources/static/js/vehicle-history.js`
- Modify: `src/main/resources/static/js/report-filters.js`

**Interfaces:**
- Consumes: Extended API payloads from Task 3, 4, 5
- Produces: Streamlined return form with odometer capture, compliance badges in vehicle list and dossier, and distance column in missions report.

- [ ] **Step 1: Streamline Pickup & Return forms in `index.html`**

In `#formPickup`:
- User selection, vehicle plate selection, destination, and optional notes (NO odometer or fuel inputs).

In `#formReturn`:
- User selection.
- `input[name="returnOdometer"]` (کیلومتر ثبت‌شده روی کیلومترشمار هنگام تحویل خودرو - الزامی).
- `textarea[name="returnNotes"]` (توضیحات تحویل / خسارت یا گزارش خرابی احتمالی - اختیاری).

- [ ] **Step 2: Update `api.js` and `app.js` to serialize `returnOdometer` and display trip distance**

In `app.js`:
- Submit `returnOdometer` with return request.
- Display `distanceKm` in the missions table (`#activeRentalsTable` and `#reportTable`).

- [ ] **Step 3: Update `vehicle-history.js` to render compliance tab & badges**

- Render badge pills: `بیمه منقضی` (قرمز)، `سررسید بیمه` (زرد)، `معاینه فنی منقضی` (نارنجی).
- Add "اطلاعات بیمه و معاینه فنی" tab or section inside the vehicle history modal with edit form.

- [ ] **Step 4: Update Excel / CSV export in `ReportExcelExporter.java` and `report-filters.js`**

Add "کیلومتر شروع"، "کیلومتر برگشت"، و "مسافت طی شده (کیلومتر)" columns to CSV and Excel exports.

- [ ] **Step 5: Verify build & tests pass**

Run: `mvn test`
Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/ src/com/car/rental/util/ReportExcelExporter.java
git commit -m "feat(ui): add return odometer input, trip distance display and compliance badges"
```

---

## Plan Review Checklist

1. **Spec coverage:** Covers return-only odometer tracking, auto-sync, compliance monitoring, Jakarta validation, and OpenAPI documentation.
2. **Strategy compliance:** Completely removes fuel tracking and pickup odometer inputs to optimize for the Software-Only edition.
3. **Step scan:** Each task has distinct failing test, implementation, verification, and commit steps.
4. **Type consistency:** Identical field names (`pickupOdometer`, `returnOdometer`, `distanceKm`, `insuranceExpiryDate`) across DTOs, entities, and UI scripts.
5. **Review Focus:** Odometer progression, current odometer sync, Bean Validation Persian formatting, compliance expiry, and Swagger UI security are directly addressed.
