package com.car.rental.db;

import com.car.rental.model.VehicleMissionReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VehicleMissionReportRepositoryTest {

    @Autowired
    private VehicleMissionReportRepository repository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void testInsertAndQueryReports() throws Exception {
        com.car.rental.model.Vehicle v = vehicleRepository.listAllVehicles().stream().findFirst().orElse(null);
        if (v == null) return;
        VehicleRepository.VehicleRow row = vehicleRepository.findRowByPlate(v.getPlate());
        if (row == null) return;

        VehicleMissionReport report = new VehicleMissionReport(
                0, null, row.id, null,
                "FUEL", "بنزین مأموریت", "۳۰ لیتر بنزین",
                900000, 30.0, null, null, false, null,
                "1405-01-01", null
        );

        int id = repository.insert(report);
        assertTrue(id > 0);

        List<VehicleMissionReport> reports = repository.findByVehicleId(row.id);
        assertFalse(reports.isEmpty());
        assertTrue(reports.stream().anyMatch(r -> r.getId() == id && "FUEL".equals(r.getReportType())));
    }
}
