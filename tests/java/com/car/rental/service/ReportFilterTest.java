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
import static com.car.rental.support.TestDataSeed.EMP_BUSY_ID;
import static com.car.rental.support.TestDataSeed.EMP_BUSY_NAME;
import static com.car.rental.support.TestDataSeed.EMP_FREE_ID;
import static com.car.rental.support.TestDataSeed.EMP_HODA_ID;
import static com.car.rental.support.TestDataSeed.EMP_OMID_ID;
import static com.car.rental.support.TestDataSeed.EMP_REZA_ID;
import static com.car.rental.support.TestDataSeed.HISTORY_CLOSED_TRIPS;
import static com.car.rental.support.TestDataSeed.HISTORY_OPEN_TRIPS;
import static com.car.rental.support.TestDataSeed.HISTORY_TOTAL_TRIPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses {@link TestDataSeed#seedWithHistory}. */
class ReportFilterTest {

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
    void fullHistoryHasExpectedTripCount() throws Exception {
        assertEquals(HISTORY_TOTAL_TRIPS, testDb.rentals().getRentalReport().size());
    }

    @Test
    void filterOpenOnly() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setStatus(RentalReportFilter.Status.OPEN);
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(HISTORY_OPEN_TRIPS, rows.size());
        assertTrue(rows.stream().anyMatch(r -> EMP_BUSY_ID.equals(r.deviceUserId)));
        assertTrue(rows.stream().anyMatch(r -> EMP_OMID_ID.equals(r.deviceUserId)));
        assertTrue(rows.stream().anyMatch(r -> EMP_HODA_ID.equals(r.deviceUserId)));
    }

    @Test
    void filterClosedOnly() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setStatus(RentalReportFilter.Status.CLOSED);
        assertEquals(HISTORY_CLOSED_TRIPS, testDb.rentals().getRentalReport(f).size());
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
    void freeEmployeeHasTwoClosedTripsInHistory() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setEmployeeName("Free");
        f.setStatus(RentalReportFilter.Status.CLOSED);
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> EMP_FREE_ID.equals(r.deviceUserId)));
    }

    @Test
    void singleDayWithTwoClosedTrips() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setDateFrom("1405/05/25");
        f.setDateTo("1405/05/25");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(2, rows.size());
    }

    @Test
    void singleDayOpenTripsOn26() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setDateFrom("1405/05/26");
        f.setDateTo("1405/05/26");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        // Busy + Omid open from 26; Hoda starts 27 so not on 26
        assertEquals(2, rows.size());
    }

    @Test
    void multiDayRangeSpansOvernightTrip() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setDateFrom("1405/05/19");
        f.setDateTo("1405/05/20");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertTrue(rows.stream().anyMatch(r -> EMP_REZA_ID.equals(r.deviceUserId)));
    }

    @Test
    void wideRangeReturnsMostHistory() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setDateFrom("1405/05/18");
        f.setDateTo("1405/05/27");
        assertEquals(HISTORY_TOTAL_TRIPS, testDb.rentals().getRentalReport(f).size());
    }

    @Test
    void filterByDestination() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setDestination("Isfahan");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).destination.contains("Isfahan"));
    }

    @Test
    void filterByPlate() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setPlate(CAR_BUSY_PLATE);
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertEquals(EMP_BUSY_ID, rows.get(0).deviceUserId);
    }

    @Test
    void combinedNameAndStatus() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setEmployeeName("Omid");
        f.setStatus(RentalReportFilter.Status.OPEN);
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(1, rows.size());
        assertEquals(EMP_OMID_ID, rows.get(0).deviceUserId);
    }

    @Test
    void carNameFilterFindsPrideTrips() throws Exception {
        RentalReportFilter f = new RentalReportFilter();
        f.setCarName("Pride");
        List<RentalRecord> rows = testDb.rentals().getRentalReport(f);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> EMP_FREE_ID.equals(r.deviceUserId)));
    }
}
