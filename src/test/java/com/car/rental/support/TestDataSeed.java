package com.car.rental.support;

import com.car.rental.db.DatabaseManager;

import java.sql.SQLException;

/**
 * Fixed fixture data for domain tests.
 *
 * <pre>
 * Employees:
 *   1001  Free Employee     (آزاد)
 *   1002  Busy Employee     (will be put on mission in scenarios)
 *   1003  Soft-deleted      (is_active=0) — only if needed via direct SQL later
 *
 * Cars:
 *   plate FREE-01   free
 *   plate BUSY-01   free until rented in scenario
 * </pre>
 */
public final class TestDataSeed {

    public static final String EMP_FREE_ID = "1001";
    public static final String EMP_BUSY_ID = "1002";
    public static final String EMP_FREE_NAME = "Free Employee";
    public static final String EMP_BUSY_NAME = "Busy Employee";

    public static final String CAR_FREE_PLATE = "11B22233";
    public static final String CAR_BUSY_PLATE = "22C33344";
    public static final String CAR_FREE_NAME = "Pride";
    public static final String CAR_BUSY_NAME = "Samand";

    private TestDataSeed() {
    }

    public static void seedBase(DatabaseManager db) throws SQLException {
        db.addEmployee(EMP_FREE_ID, EMP_FREE_NAME, "09120000001");
        db.addEmployee(EMP_BUSY_ID, EMP_BUSY_NAME, "09120000002");
        db.addCar(CAR_FREE_NAME, CAR_FREE_PLATE, "White");
        db.addCar(CAR_BUSY_NAME, CAR_BUSY_PLATE, "Black");
    }
}
