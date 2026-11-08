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

    @Test
    @DisplayName("isOnMission returns true if and only if vehicle is non-null and rented")
    void testIsOnMission() {
        assertFalse(VehicleController.isOnMission(null));

        // Vehicle marked as rented=true even if presentation string is "آزاد"
        Vehicle v1 = new Vehicle("Pride", "11B22233", "White", "CAR", "آزاد");
        v1.setRented(true);
        assertTrue(VehicleController.isOnMission(v1));

        // Vehicle marked as rented=false even if presentation string is "در مأموریت"
        Vehicle v2 = new Vehicle("Samand", "22B33344", "Black", "CAR", "در مأموریت");
        v2.setRented(false);
        assertFalse(VehicleController.isOnMission(v2));
    }
}
