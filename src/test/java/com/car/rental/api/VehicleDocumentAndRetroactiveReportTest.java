package com.car.rental.api;

import com.car.rental.api.dto.CreateMissionReportRequest;
import com.car.rental.api.dto.VehicleDocumentDto;
import com.car.rental.api.dto.VehicleMissionReportDto;
import com.car.rental.db.EmployeeRepository;
import com.car.rental.db.RentalRepository;
import com.car.rental.db.VehicleRepository;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.Vehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VehicleDocumentAndRetroactiveReportTest {

    @Autowired
    private VehicleDocumentController documentController;

    @Autowired
    private VehicleHistoryController historyController;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void testVehicleDocumentCrud() throws Exception {
        Vehicle v = vehicleRepository.listAllVehicles().stream().findFirst().orElse(null);
        if (v == null) return;

        VehicleDocumentDto dto = new VehicleDocumentDto();
        dto.setDocumentType("THIRD_PARTY_INSURANCE");
        dto.setDocumentNumber("INS-9999");
        dto.setIssueDate("1404-01-01");
        dto.setExpiryDate("1405-01-01");
        dto.setNotes("بیمه ایران");

        VehicleDocumentDto created = documentController.addDocument(v.getPlate(), dto);
        assertNotNull(created);
        assertTrue(created.getId() > 0);

        List<VehicleDocumentDto> list = documentController.list(v.getPlate());
        assertTrue(list.stream().anyMatch(d -> d.getId() == created.getId()));

        var delRes = documentController.deleteDocument(v.getPlate(), created.getId());
        assertTrue(delRes.isOk());

        List<VehicleDocumentDto> listAfter = documentController.list(v.getPlate());
        assertFalse(listAfter.stream().anyMatch(d -> d.getId() == created.getId()));
    }

    @Test
    void testRetroactiveMissionReport() throws Exception {
        List<RentalRecord> allRentals = rentalRepository.getRentalReport();
        if (allRentals.isEmpty()) return;

        RentalRecord rental = allRentals.get(0);
        assertTrue(rental.id > 0);

        CreateMissionReportRequest req = new CreateMissionReportRequest();
        req.setReportType("FINE");
        req.setFineType("SPEED");
        req.setAmount(150000.0);
        req.setDescription("سرعت غیرمجاز در اتوبان");
        req.setReportDate("1404-05-10");

        VehicleMissionReportDto repDto = historyController.addRetroactiveReport(rental.id, req);
        assertNotNull(repDto);
        assertTrue(repDto.getId() > 0);
        assertFalse(repDto.isPaid());

        var payRes = historyController.payMissionFine(repDto.getId());
        assertTrue(payRes.isOk());
    }

    @Test
    void testVehicleHistoryRentalsContainReports() throws Exception {
        List<RentalRecord> allRentals = rentalRepository.getRentalReport();
        if (allRentals.isEmpty()) return;
        RentalRecord rental = allRentals.get(0);

        CreateMissionReportRequest req = new CreateMissionReportRequest();
        req.setReportType("FUEL");
        req.setLiters(28.5);
        req.setCost(85500.0);
        req.setDescription("بنزین مأموریت تستی");
        historyController.addRetroactiveReport(rental.id, req);

        var history = historyController.getHistory(rental.plate);
        assertNotNull(history);
        var rentalDto = history.getRentals().stream().filter(r -> r.getId() == rental.id).findFirst().orElse(null);
        assertNotNull(rentalDto);
        assertFalse(rentalDto.getReports().isEmpty(), "Rental reports must be populated in vehicle history!");
        assertTrue(rentalDto.getReports().stream().anyMatch(r ->
                "FUEL".equals(r.getReportType()) && r.getLiters() == 28.5 && r.getAmount() == 85500.0 && r.getCost() == 85500.0));
    }
}
