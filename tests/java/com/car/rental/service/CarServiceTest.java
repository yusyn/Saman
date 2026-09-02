package com.car.rental.service;

import com.car.rental.support.TestDataSeed;
import com.car.rental.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static com.car.rental.support.TestDataSeed.CAR_FREE_PLATE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarServiceTest {

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
    void rejectsDuplicatePlate() {
        SQLException ex = assertThrows(SQLException.class,
                () -> testDb.cars().addCar("Other", CAR_FREE_PLATE, "Red"));
        assertTrue(ex.getMessage().contains("پلاک"));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> testDb.cars().addCar("  ", "99X99999", "Blue"));
    }

    @Test
    void nextDeviceUserIdStartsFromSeedMaxPlusOne() throws Exception {
        String next = testDb.db().getNextDeviceUserId();
        assertTrue(Integer.parseInt(next) >= 1003);
    }
}
