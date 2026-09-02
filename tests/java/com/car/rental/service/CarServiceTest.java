package com.car.rental.service;

import com.car.rental.model.Car;
import com.car.rental.support.TestDataSeed;
import com.car.rental.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static com.car.rental.support.TestDataSeed.CAR_COUNT;
import static com.car.rental.support.TestDataSeed.CAR_FREE_PLATE;
import static com.car.rental.support.TestDataSeed.CAR_PEUGEOT_PLATE;
import static com.car.rental.support.TestDataSeed.CAR_TIBA_PLATE;
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
    void seedListsAllCarsAsAvailable() throws Exception {
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
    void addNewCarIncreasesFleet() throws Exception {
        testDb.cars().addCar("L90", "77H88899", "Green");
        assertEquals(CAR_COUNT + 1, testDb.cars().getAllCars().size());
    }

    @Test
    void updateCarChangesPlateAndModel() throws Exception {
        Car car = new Car("Peugeot 207", "88J99900", "White", "آزاد");
        testDb.cars().updateCar(car, CAR_PEUGEOT_PLATE);

        List<Car> all = testDb.cars().getAllCars();
        assertTrue(all.stream().anyMatch(c -> "88J99900".equals(c.getPlate())));
        assertTrue(all.stream().noneMatch(c -> CAR_PEUGEOT_PLATE.equals(c.getPlate())));
    }

    @Test
    void softDeletedCarNotInActiveLists() throws Exception {
        testDb.cars().deleteCar(CAR_TIBA_PLATE);
        assertEquals(CAR_COUNT - 1, testDb.cars().getAllCars().size());
        assertTrue(testDb.cars().getAvailableCars().stream()
                .noneMatch(c -> CAR_TIBA_PLATE.equals(c.getPlate())));
    }

    @Test
    void nextDeviceUserIdStartsFromSeedMaxPlusOne() throws Exception {
        String next = testDb.db().getNextDeviceUserId();
        assertEquals("1009", next);
    }
}
