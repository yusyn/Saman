package com.car.rental.support;

import com.car.rental.db.DatabaseManager;

import java.sql.SQLException;

/**
 * Fixed fixture data for domain tests.
 *
 * <p>{@link #seedBase} — catalog only (all free).
 * <p>{@link #seedWithHistory} — catalog + closed/open trips across several days.
 */
public final class TestDataSeed {

    // --- employees ---
    public static final String EMP_FREE_ID = "1001";
    public static final String EMP_BUSY_ID = "1002";
    public static final String EMP_REZA_ID = "1003";
    public static final String EMP_SARA_ID = "1004";
    public static final String EMP_OMID_ID = "1005";
    public static final String EMP_NIMA_ID = "1006";
    public static final String EMP_LEILA_ID = "1007";
    public static final String EMP_KARIM_ID = "1008";
    public static final String EMP_PARSA_ID = "1009";
    public static final String EMP_HODA_ID = "1010";
    public static final String EMP_ARIA_ID = "1011";
    public static final String EMP_YAS_ID = "1012";

    public static final String EMP_FREE_NAME = "Free Employee";
    public static final String EMP_BUSY_NAME = "Busy Employee";
    public static final String EMP_REZA_NAME = "Reza Ahmadi";
    public static final String EMP_SARA_NAME = "Sara Karimi";
    public static final String EMP_OMID_NAME = "Omid Hosseini";
    public static final String EMP_NIMA_NAME = "Nima Jafari";
    public static final String EMP_LEILA_NAME = "Leila Mohammadi";
    public static final String EMP_KARIM_NAME = "Karim Nazari";
    public static final String EMP_PARSA_NAME = "Parsa Rahimi";
    public static final String EMP_HODA_NAME = "Hoda Salehi";
    public static final String EMP_ARIA_NAME = "Aria Ghorbani";
    public static final String EMP_YAS_NAME = "Yasmin Farhadi";

    // --- cars ---
    public static final String CAR_FREE_PLATE = "11B22233";
    public static final String CAR_BUSY_PLATE = "22C33344";
    public static final String CAR_PEUGEOT_PLATE = "33D44455";
    public static final String CAR_DENA_PLATE = "44E55566";
    public static final String CAR_QUICK_PLATE = "55F66677";
    public static final String CAR_TIBA_PLATE = "66G77788";
    public static final String CAR_RUNNA_PLATE = "77H88899";
    public static final String CAR_SINA_PLATE = "88J99900";
    public static final String CAR_ARIZO_PLATE = "99K11122";
    public static final String CAR_SHAHIN_PLATE = "12L33344";

    public static final String CAR_FREE_NAME = "Pride";
    public static final String CAR_BUSY_NAME = "Samand";
    public static final String CAR_PEUGEOT_NAME = "Peugeot 206";
    public static final String CAR_DENA_NAME = "Dena";
    public static final String CAR_QUICK_NAME = "Quick";
    public static final String CAR_TIBA_NAME = "Tiba";
    public static final String CAR_RUNNA_NAME = "Runna";
    public static final String CAR_SINA_NAME = "Saina";
    public static final String CAR_ARIZO_NAME = "Arizo 5";
    public static final String CAR_SHAHIN_NAME = "Shahin";

    public static final int EMPLOYEE_COUNT = 12;
    public static final int CAR_COUNT = 10;
    /** Closed trips created by {@link #seedWithHistory}. */
    public static final int HISTORY_CLOSED_TRIPS = 8;
    /** Open trips left active by {@link #seedWithHistory}. */
    public static final int HISTORY_OPEN_TRIPS = 3;
    public static final int HISTORY_TOTAL_TRIPS = HISTORY_CLOSED_TRIPS + HISTORY_OPEN_TRIPS;

    private TestDataSeed() {
    }

    /** Employees + cars only. Everyone free / available. */
    public static void seedBase(DatabaseManager db) throws SQLException {
        db.addEmployee(EMP_FREE_ID, EMP_FREE_NAME, "09120000001");
        db.addEmployee(EMP_BUSY_ID, EMP_BUSY_NAME, "09120000002");
        db.addEmployee(EMP_REZA_ID, EMP_REZA_NAME, "09120000003");
        db.addEmployee(EMP_SARA_ID, EMP_SARA_NAME, "09120000004");
        db.addEmployee(EMP_OMID_ID, EMP_OMID_NAME, "09120000005");
        db.addEmployee(EMP_NIMA_ID, EMP_NIMA_NAME, "09120000006");
        db.addEmployee(EMP_LEILA_ID, EMP_LEILA_NAME, "09120000007");
        db.addEmployee(EMP_KARIM_ID, EMP_KARIM_NAME, "09120000008");
        db.addEmployee(EMP_PARSA_ID, EMP_PARSA_NAME, "09120000009");
        db.addEmployee(EMP_HODA_ID, EMP_HODA_NAME, "09120000010");
        db.addEmployee(EMP_ARIA_ID, EMP_ARIA_NAME, "09120000011");
        db.addEmployee(EMP_YAS_ID, EMP_YAS_NAME, "09120000012");

        db.addCar(CAR_FREE_NAME, CAR_FREE_PLATE, "White");
        db.addCar(CAR_BUSY_NAME, CAR_BUSY_PLATE, "Black");
        db.addCar(CAR_PEUGEOT_NAME, CAR_PEUGEOT_PLATE, "Silver");
        db.addCar(CAR_DENA_NAME, CAR_DENA_PLATE, "Blue");
        db.addCar(CAR_QUICK_NAME, CAR_QUICK_PLATE, "Red");
        db.addCar(CAR_TIBA_NAME, CAR_TIBA_PLATE, "Gray");
        db.addCar(CAR_RUNNA_NAME, CAR_RUNNA_PLATE, "White");
        db.addCar(CAR_SINA_NAME, CAR_SINA_PLATE, "Black");
        db.addCar(CAR_ARIZO_NAME, CAR_ARIZO_PLATE, "Blue");
        db.addCar(CAR_SHAHIN_NAME, CAR_SHAHIN_PLATE, "Gray");
    }

    /**
     * Catalog + multi-day history.
     *
     * <pre>
     * Closed (8):
     *   1001 Pride    05/18 → 05/18  Tehran
     *   1003 Peugeot  05/19 → 05/20  Qom
     *   1004 Dena     05/21 → 05/21  Karaj
     *   1006 Tiba     05/22 → 05/22  Rasht
     *   1007 Runna    05/23 → 05/23  Yazd
     *   1001 Pride    05/24 → 05/24  Tehran   (2nd trip same person)
     *   1008 Saina    05/25 → 05/25  Ahvaz
     *   1009 Arizo    05/25 → 05/25  Kerman
     * Open (3):
     *   1002 Samand   from 05/26  Isfahan
     *   1005 Quick    from 05/26  Shiraz
     *   1010 Shahin   from 05/27  Mashhad
     * </pre>
     */
    public static void seedWithHistory(DatabaseManager db) throws SQLException {
        seedBase(db);

        pickupReturn(db, EMP_FREE_ID, CAR_FREE_PLATE,
                "1405/05/18 09:00:00", "1405/05/18 17:00:00", "Tehran");
        pickupReturn(db, EMP_REZA_ID, CAR_PEUGEOT_PLATE,
                "1405/05/19 08:30:00", "1405/05/20 19:00:00", "Qom");
        pickupReturn(db, EMP_SARA_ID, CAR_DENA_PLATE,
                "1405/05/21 10:00:00", "1405/05/21 15:30:00", "Karaj");
        pickupReturn(db, EMP_NIMA_ID, CAR_TIBA_PLATE,
                "1405/05/22 07:00:00", "1405/05/22 20:00:00", "Rasht");
        pickupReturn(db, EMP_LEILA_ID, CAR_RUNNA_PLATE,
                "1405/05/23 11:00:00", "1405/05/23 16:00:00", "Yazd");
        pickupReturn(db, EMP_FREE_ID, CAR_FREE_PLATE,
                "1405/05/24 09:00:00", "1405/05/24 17:00:00", "Tehran");
        pickupReturn(db, EMP_KARIM_ID, CAR_SINA_PLATE,
                "1405/05/25 08:00:00", "1405/05/25 18:00:00", "Ahvaz");
        pickupReturn(db, EMP_PARSA_ID, CAR_ARIZO_PLATE,
                "1405/05/25 12:00:00", "1405/05/25 21:00:00", "Kerman");

        db.insertRental(EMP_BUSY_ID, CAR_BUSY_PLATE, "1405/05/26 08:00:00", "Isfahan");
        db.insertRental(EMP_OMID_ID, CAR_QUICK_PLATE, "1405/05/26 11:00:00", "Shiraz");
        db.insertRental(EMP_HODA_ID, CAR_SHAHIN_PLATE, "1405/05/27 09:30:00", "Mashhad");
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
