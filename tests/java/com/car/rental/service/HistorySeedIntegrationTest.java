package com.car.rental.service;

import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.support.TestDataSeed;
import com.car.rental.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.car.rental.support.TestDataSeed.CAR_BUSY_PLATE;
import static com.car.rental.support.TestDataSeed.CAR_COUNT;
import static com.car.rental.support.TestDataSeed.CAR_FREE_PLATE;
import static com.car.rental.support.TestDataSeed.CAR_QUICK_PLATE;
import static com.car.rental.support.TestDataSeed.EMP_BUSY_ID;
import static com.car.rental.support.TestDataSeed.EMP_COUNT;
import static com.car.rental.support.TestDataSeed.EMP_FREE_ID;
import static com.car.rental.support.TestDataSeed.EMP_OMID_ID;
import static com.car.rental.support.TestDataSeed.EMP_SARA_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the richer {@link TestDataSeed#seedWithHistory} fixture as a whole.
 */
class HistorySeedIntegrationTest {

    private TestDb testDb;

    @BeforeEach
    void setUp() throws Exception {
        testDb = new TestDb();
        TestDataSeed.seedWithHistory(testDb.db());
    }

    @AfterEach
    void tearDown() {
        if (testDb != null) {
            testDb.close();
        }
    }

    @Test
    void catalogSizesUnchangedByHistory() throws Exception {
        assertEquals(EMP_COUNT, testDb.db().getAllEmployees().size());
        assertEquals(CAR_COUNT, testDb.cars().getAllCars().size());
    }

    @Test
    void afterHistoryTwoCarsOnMissionFourFree() throws Exception {
        List<Car> free = testDb.cars().getAvailableCars();
        assertEquals(4, free.size());
        assertTrue(free.stream().anyMatch(c -> CAR_FREE_PLATE.equals(c.getPlate())));
        assertTrue(free.stream().noneMatch(c -> CAR_BUSY_PLATE.equals(c.getPlate())));
        assertTrue(free.stream().noneMatch(c -> CAR_QUICK_PLATE.equals(c.getPlate())));
    }

    @Test
    void busyAndOmidAreRentingOthersAreNot() throws Exception {
        assertTrue(testDb.db().findByDeviceUserId(EMP_BUSY_ID).isRenting());
        assertTrue(testDb.db().findByDeviceUserId(EMP_OMID_ID).isRenting());
        assertFalse(testDb.db().findByDeviceUserId(EMP_FREE_ID).isRenting());
        assertFalse(testDb.db().findByDeviceUserId(EMP_SARA_ID).isRenting());
    }

    @Test
    void activeRentalsOnlyForOpenTrips() throws Exception {
        assertNotNull(testDb.rentals().getActiveRentalByDeviceUserId(EMP_BUSY_ID));
        assertNotNull(testDb.rentals().getActiveRentalByDeviceUserId(EMP_OMID_ID));
        assertNull(testDb.rentals().getActiveRentalByDeviceUserId(EMP_FREE_ID));
    }

    @Test
    void freeEmployeeCanStartNewTripWhileOthersStillOut() throws Exception {
        testDb.rentals().pickup(EMP_FREE_ID, CAR_FREE_PLATE, "1405/05/27 09:00:00", "Mashhad");
        RentalRecord active = testDb.rentals().getActiveRentalByDeviceUserId(EMP_FREE_ID);
        assertNotNull(active);
        assertEquals("Mashhad", active.destination);
        assertEquals(3, testDb.cars().getAvailableCars().size());
    }

    @Test
    void returningOneOpenTripDoesNotAffectTheOther() throws Exception {
        assertTrue(testDb.rentals().returnCar(EMP_BUSY_ID, "1405/05/27 18:00:00"));

        Employee busy = testDb.db().findByDeviceUserId(EMP_BUSY_ID);
        assertFalse(busy.isRenting());
        assertNull(testDb.rentals().getActiveRentalByDeviceUserId(EMP_BUSY_ID));

        assertTrue(testDb.db().findByDeviceUserId(EMP_OMID_ID).isRenting());
        assertNotNull(testDb.rentals().getActiveRentalByDeviceUserId(EMP_OMID_ID));
        assertEquals(5, testDb.cars().getAvailableCars().size());
    }
}
