package com.car.rental.support;

import com.car.rental.db.DatabaseManager;

import java.sql.SQLException;

/**
 * Fixed fixture data for domain tests.
 *
 * <p>{@link #seedBase} = catalog only (employees + cars, no trips).
 * <p>{@link #seedWithHistory} = catalog + several closed/open rentals across days.
 */
public final class TestDataSeed {

    // --- employees (device_user_id) ---
    public static final String EMP_FREE_ID = "1001";
    public static final String EMP_BUSY_ID = "1002";
    public static final String EMP_REZA_ID = "1003";
    public static final String EMP_SARA_ID = "1004";
    public static final String EMP_OMID_ID = "1005";
    public static final String EMP_NIMA_ID = "1006";
    public static final String EMP_LEILA_ID = "1007";
    public static final String EMP_KARIM_ID = "1008";

    public static final String EMP_FREE_NAME = "Free Employee";
    public static final String EMP_BUSY_NAME = "Busy Employee";
    public static final String EMP_REZA_NAME = "Reza Ahmadi";
    public static final String EMP_SARA_NAME = "Sara Karimi";
    public static final String EMP_OMID_NAME = "Omid Hosseini";
    public static final String EMP_NIMA_NAME = "Nima Jafari";
    public static final String EMP_LEILA_NAME = "Leila Mohammadi";
    public static final String EMP_KARIM_NAME = "Karim Nazari";

    // --- cars ---
    public static final String CAR_FREE_PLATE = "11B22233";
    public static final String CAR_BUSY_PLATE = "22C33344";
    public static final String CAR_PEUGEOT_PLATE = "33D44455";
    public static final String CAR_DENA_PLATE = "44E55566";
    public static final String CAR_QUICK_PLATE = "55F66677";
    public static final String CAR_TIBA_PLATE = "66G77788";

    public static final String CAR_FREE_NAME = "Pride";
    public static final String CAR_BUSY_NAME = "Samand";
    public static final String CAR_PEUGEOT_NAME = "Peugeot 206";
    public static final String CAR_DENA_NAME = "Dena";
    public static final String CAR_QUICK_NAME = "Quick";
    public static final String CAR_TIBA_NAME = "Tiba";

    public static final int EMPLOYEE_COUNT = 8;
    public static final int CAR_COUNT = 6;

    private TestDataSeed() {
    }

    /** Employees + cars only. All free / available. */
    public static void seedBase(DatabaseManager db) throws SQLException {
        db.addEmployee(EMP_FREE_ID, EMP_FREE_NAME, "09120000001");
        db.addEmployee(EMP_BUSY_ID, EMP_BUSY_NAME, "09120000002");
        db.addEmployee(EMP_REZA_ID, EMP_REZA_NAME, "09120000003");
        db.addEmployee(EMP_SARA_ID, EMP_SARA_NAME, "09120000004");
        db.addEmployee(EMP_OMID_ID, EMP_OMID_NAME, "09120000005");
        db.addEmployee(EMP_NIMA_ID, EMP_NIMA_NAME, "09120000006");
        db.addEmployee(EMP_LEILA_ID, EMP_LEILA_NAME, "09120000007");
        db.addEmployee(EMP_KARIM_ID, EMP_KARIM_NAME, "09120000008");

        db.addCar(CAR_FREE_NAME, CAR_FREE_PLATE, "White");
        db.addCar(CAR_BUSY_NAME, CAR_BUSY_PLATE, "Black");
        db.addCar(CAR_PEUGEOT_NAME, CAR_PEUGEOT_PLATE, "Silver");
        db.addCar(CAR_DENA_NAME, CAR_DENA_PLATE, "Blue");
        db.addCar(CAR_QUICK_NAME, CAR_QUICK_PLATE, "Red");
        db.addCar(CAR_TIBA_NAME, CAR_TIBA_PLATE, "Gray");
    }

    /**
     * Catalog + multi-day rental history for report and concurrency scenarios.
     *
     * <pre>
     * Closed:
     *   1001 Pride   1405/05/20 → 1405/05/20  Tehran
     *   1003 Peugeot 1405/05/22 → 1405/05/23  Qom
     *   1004 Dena    1405/05/24 → 1405/05/24  Karaj
     *   1001 Pride   1405/05/25 → 1405/05/25  Tehran   (second trip same person)
     * Open (still on mission after seed):
     *   1002 Samand  from 1405/05/26  Isfahan
     *   1005 Quick   from 1405/05/26  Shiraz
     * </pre>
     */
    public static void seedWithHistory(DatabaseManager db) throws SQLException {
        seedBase(db);

        // Closed trips
        pickupReturn(db, EMP_FREE_ID, CAR_FREE_PLATE,
                "1405/05/20 09:00:00", "1405/05/20 17:00:00", "Tehran");
        pickupReturn(db, EMP_REZA_ID, CAR_PEUGEOT_PLATE,
                "1405/05/22 08:30:00", "1405/05/23 19:00:00", "Qom");
        pickupReturn(db, EMP_SARA_ID, CAR_DENA_PLATE,
                "1405/05/24 10:00:00", "1405/05/24 15:30:00", "Karaj");
        pickupReturn(db, EMP_FREE_ID, CAR_FREE_PLATE,
                "1405/05/25 09:00:00", "1405/05/25 17:00:00", "Tehran");

        // Open trips — leave on mission
        db.insertRental(EMP_BUSY_ID, CAR_BUSY_PLATE, "1405/05/26 08:00:00", "Isfahan");
        db.insertRental(EMP_OMID_ID, CAR_QUICK_PLATE, "1405/05/26 11:00:00", "Shiraz");
    }

    private static void pickupReturn(DatabaseManager db,
                                     String empId,
                                     String plate,
                                     String pickup,
                                     String ret,
                                     String dest) throws SQLException {
        db.insertRental(empId, plate, pickup, dest);
        db.returnCarByDeviceUserId(empId, ret);
    }
}
