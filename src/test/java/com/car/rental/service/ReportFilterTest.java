package com.car.rental.service;

import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import com.car.rental.support.TestDataSeed;
import com.car.rental.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.car.rental.support.TestDataSeed.CAR_BUSY_PLATE;
import static com.car.rental.support.TestDataSeed.CAR_FREE_PLATE;
import static com.car.rental.support.TestDataSeed.EMP_BUSY_ID;
import static com.car.rental.support.TestDataSeed.EMP_BUSY_NAME;
import static com.car.rental.support.TestDataSeed.EMP_FREE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFilterTest {

    private TestDb testDb;

    @BeforeEach
    void setUp() throws Exception {
        testDb = new TestDb();
        TestDataSeed.seedBase(testDb.db());

        // Closed trip on day 25
        testDb.rentals().pickup(EMP_FREE_ID, CAR_FREE_PLATE, "1405/05/25 09:00:00", "Tehran");
        testDb.rentals().returnCar(EMP_FREE_ID, "1405/05/25 17:00:00");

        // Open trip starting day 26
        testDb.rentals().pickup(EMP_BUSY_ID, CAR_BUSY_PLATE, "1405/05/26 08:00:00", "Isfahan");
    }

    @AfterEach
    void tearDown() {
        if (testDb != null) {
            testDb.close();
        }
    }

    @Test
    void filterOpenOnly() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setStatus(RentalReportFilter.Status.OPEN);
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertEquals(EMP_BUSY_ID, rows.get(0).deviceUserId);
    }

    @Test
    void filterClosedOnly() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setStatus(RentalReportFilter.Status.CLOSED);
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertEquals(EMP_FREE_ID, rows.get(0).deviceUserId);
    }

    @Test
    void filterByEmployeeName() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setEmployeeName("Busy");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertEquals(EMP_BUSY_NAME, rows.get(0).employeeName);
    }

    @Test
    void singleDayOverlapIncludesOpenTripStillActive() throws Exception {
        // Who had a car on 1405/05/26?
        RentalReportFilter f = new RentalReportFilter();
        f.setDateFrom("1405/05/26");
        f.setDateTo("1405/05/26");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertEquals(EMP_BUSY_ID, rows.get(0).deviceUserId);
    }

    @Test
    void singleDayIncludesCompletedTripThatDay() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setDateFrom("1405/05/25");
        f.setDateTo("1405/05/25");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertEquals(EMP_FREE_ID, rows.get(0).deviceUserId);
    }

    @Test
    void filterByDestination() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setDestination("Isfahan");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).destination.contains("Isfahan"));
    }
}
