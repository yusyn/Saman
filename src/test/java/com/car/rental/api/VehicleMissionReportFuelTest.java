package com.car.rental.api;

import com.car.rental.api.dto.CreateMissionReportRequest;
import com.car.rental.api.dto.VehicleMissionReportDto;
import com.car.rental.db.RentalRepository;
import com.car.rental.model.RentalRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VehicleMissionReportFuelTest {

    @Autowired
    private VehicleHistoryController historyController;

    @Autowired
    private RentalRepository rentalRepository;

    @Test
    void testFuelReportLitersAndCostDecoupled() throws Exception {
        List<RentalRecord> allRentals = rentalRepository.getRentalReport();
        assertFalse(allRentals.isEmpty(), "At least one rental should exist for tests");
        RentalRecord rental = allRentals.get(0);

        CreateMissionReportRequest fuelReq = new CreateMissionReportRequest();
        fuelReq.setReportType("FUEL");
        fuelReq.setTitle("بنزین");
        fuelReq.setLiters(35.5);
        fuelReq.setCost(105000.0);
        fuelReq.setDescription("جایگاه آزادی");

        VehicleMissionReportDto created = historyController.addRetroactiveReport(rental.id, fuelReq);
        assertNotNull(created);
        assertEquals(35.5, created.getLiters(), 0.001, "Fuel volume in liters must be 35.5");
        assertEquals(105000.0, created.getCost(), 0.001, "Fuel cost must be 105000.0");
        assertEquals(105000.0, created.getAmount(), 0.001, "Monetary amount must be 105000.0");
    }
}
