package com.car.rental.service;

import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.support.TestDataSeed;
import com.car.rental.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static com.car.rental.support.TestDataSeed.CAR_BUSY_PLATE;
import static com.car.rental.support.TestDataSeed.CAR_FREE_PLATE;
import static com.car.rental.support.TestDataSeed.EMP_BUSY_ID;
import static com.car.rental.support.TestDataSeed.EMP_FREE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Core business path: pickup → on mission → return → free again.
 */
class RentalLifecycleTest {

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
    void pickupMarksEmployeeAndCarOnMission() throws Exception {
        testDb.rentals().pickup(EMP_FREE_ID, CAR_FREE_PLATE, "1405/05/25 10:00:00", "Tehran");

        Employee emp = testDb.db().findByDeviceUserId(EMP_FREE_ID);
        assertNotNull(emp);
        assertTrue(emp.isRenting());

        List<Car> available = testDb.cars().getAvailableCars();
        assertTrue(available.stream().noneMatch(c -> CAR_FREE_PLATE.equals(c.getPlate())));

        RentalRecord active = testDb.rentals().getActiveRentalByDeviceUserId(EMP_FREE_ID);
        assertNotNull(active);
        assertEquals(CAR_FREE_PLATE, active.plate);
        assertNull(active.returnDate);
    }

    @Test
    void returnFreesEmployeeAndCar() throws Exception {
        testDb.rentals().pickup(EMP_FREE_ID, CAR_FREE_PLATE, "1405/05/25 10:00:00", "Tehran");
        boolean ok = testDb.rentals().returnCar(EMP_FREE_ID, "1405/05/25 18:00:00");
        assertTrue(ok);

        Employee emp = testDb.db().findByDeviceUserId(EMP_FREE_ID);
        assertFalse(emp.isRenting());

        List<Car> available = testDb.cars().getAvailableCars();
        assertTrue(available.stream().anyMatch(c -> CAR_FREE_PLATE.equals(c.getPlate())));

        assertNull(testDb.rentals().getActiveRentalByDeviceUserId(EMP_FREE_ID));
    }

    @Test
    void cannotPickupWhenEmployeeAlreadyOnMission() throws Exception {
        testDb.rentals().pickup(EMP_FREE_ID, CAR_FREE_PLATE, "1405/05/25 10:00:00", "Tehran");

        SQLException ex = assertThrows(SQLException.class,
                () -> testDb.rentals().pickup(EMP_FREE_ID, CAR_BUSY_PLATE, "1405/05/25 11:00:00", "Qom"));
        assertTrue(ex.getMessage().contains("مأموریت") || ex.getMessage().contains("ماموریت"));
    }

    @Test
    void cannotPickupWhenCarAlreadyRented() throws Exception {
        testDb.rentals().pickup(EMP_FREE_ID, CAR_FREE_PLATE, "1405/05/25 10:00:00", "Tehran");

        SQLException ex = assertThrows(SQLException.class,
                () -> testDb.rentals().pickup(EMP_BUSY_ID, CAR_FREE_PLATE, "1405/05/25 11:00:00", "Qom"));
        assertTrue(ex.getMessage().contains("مأموریت") || ex.getMessage().contains("ماموریت"));
    }

    @Test
    void returnWithoutActiveRentalReturnsFalse() throws Exception {
        assertFalse(testDb.rentals().returnCar(EMP_FREE_ID, "1405/05/25 18:00:00"));
    }

    @Test
    void cannotDeleteCarWhileOnMission() throws Exception {
        testDb.rentals().pickup(EMP_FREE_ID, CAR_FREE_PLATE, "1405/05/25 10:00:00", "Tehran");
        assertThrows(SQLException.class, () -> testDb.cars().deleteCar(CAR_FREE_PLATE));
    }

    @Test
    void canDeleteFreeCar() throws Exception {
        testDb.cars().deleteCar(CAR_FREE_PLATE);
        assertTrue(testDb.cars().getAllCars().stream()
                .noneMatch(c -> CAR_FREE_PLATE.equals(c.getPlate())));
    }
}
