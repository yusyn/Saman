package com.car.rental.service;

import com.car.rental.support.TestDataSeed;
import com.car.rental.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static com.car.rental.support.TestDataSeed.CAR_COUNT;
import static com.car.rental.support.TestDataSeed.CAR_FREE_PLATE;
import static com.car.rental.support.TestDataSeed.CAR_SHAHIN_PLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void seedHasTenCars() throws Exception {
        assertEquals(CAR_COUNT, testDb.cars().getAllCars().size());
        assertEquals(CAR_COUNT, testDb.cars().getAvailableCars().size());
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
        assertTrue(Integer.parseInt(next) >= 1013);
    }

    @Test
    void canAddNewCarOnTopOfSeed() throws Exception {
        testDb.cars().addCar("Tara", "15M55566", "White");
        assertEquals(CAR_COUNT + 1, testDb.cars().getAllCars().size());
        assertTrue(testDb.cars().getAvailableCars().stream()
                .anyMatch(c -> "15M55566".equals(c.getPlate())));
    }

    @Test
    void shahinExistsInSeed() throws Exception {
        assertTrue(testDb.cars().getAllCars().stream()
                .anyMatch(c -> CAR_SHAHIN_PLATE.equals(c.getPlate())));
    }
}
