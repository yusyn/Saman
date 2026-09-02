package com.car.rental.service;

import com.car.rental.model.Employee;
import com.car.rental.support.TestDataSeed;
import com.car.rental.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static com.car.rental.support.TestDataSeed.EMP_COUNT;
import static com.car.rental.support.TestDataSeed.EMP_FREE_ID;
import static com.car.rental.support.TestDataSeed.EMP_FREE_NAME;
import static com.car.rental.support.TestDataSeed.EMP_REZA_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Catalog-level employee behavior (DB only — no fingerprint device). */
class EmployeeCatalogTest {

    private TestDb testDb;

    @BeforeEach
    void setUp() throws Exception {
        testDb = new TestDb();
        TestDataSeed.seedBase(testDb.db());
    }

    @AfterEach
    void tearDown() {
        if (testDb != null) {
            testDb.close();
        }
    }

    @Test
    void listsAllSeededEmployees() throws Exception {
        List<Employee> all = testDb.db().getAllEmployees();
        assertEquals(EMP_COUNT, all.size());
    }

    @Test
    void findByDeviceUserId() throws Exception {
        Employee e = testDb.db().findByDeviceUserId(EMP_FREE_ID);
        assertNotNull(e);
        assertEquals(EMP_FREE_NAME, e.getName());
        assertFalse(e.isRenting());
    }

    @Test
    void findUnknownReturnsNull() throws Exception {
        assertNull(testDb.db().findByDeviceUserId("99999"));
    }

    @Test
    void deviceUserIdExists() throws Exception {
        assertTrue(testDb.db().isDeviceUserIdExists(EMP_REZA_ID));
        assertFalse(testDb.db().isDeviceUserIdExists("99999"));
    }

    @Test
    void cannotInsertDuplicateDeviceUserId() {
        assertThrows(SQLException.class,
                () -> testDb.db().addEmployee(EMP_FREE_ID, "Someone Else", "09121111111"));
    }

    @Test
    void softDeleteHidesFromActiveList() throws Exception {
        testDb.db().deleteEmployeeByDeviceUserId(EMP_FREE_ID);
        assertNull(testDb.db().findByDeviceUserId(EMP_FREE_ID));
        assertEquals(EMP_COUNT - 1, testDb.db().getAllEmployees().size());
        // id still reserved in table uniqueness sense
        assertTrue(testDb.db().isDeviceUserIdExists(EMP_FREE_ID));
    }

    @Test
    void nextIdAfterSeedIsAboveMax() throws Exception {
        String next = testDb.db().getNextDeviceUserId();
        assertEquals("1009", next);
    }

    @Test
    void updateEmployeeNameAndPhone() throws Exception {
        Employee e = testDb.db().findByDeviceUserId(EMP_REZA_ID);
        e.setName("Reza Updated");
        e.setPhone("09129998877");
        testDb.db().updateEmployee(e);

        Employee again = testDb.db().findByDeviceUserId(EMP_REZA_ID);
        assertEquals("Reza Updated", again.getName());
        assertEquals("09129998877", again.getPhone());
    }
}
