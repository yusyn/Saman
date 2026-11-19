package com.car.rental.api;

import com.car.rental.api.dto.CreateMissionReportRequest;
import com.car.rental.api.dto.ReturnRequest;
import com.car.rental.db.EmployeeRepository;
import com.car.rental.db.RentalRepository;
import com.car.rental.db.VehicleMissionReportRepository;
import com.car.rental.db.VehicleRepository;
import com.car.rental.model.Employee;
import com.car.rental.model.Vehicle;
import com.car.rental.model.VehicleMissionReport;
import com.car.rental.service.RentalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RentalReturnFlowTest {

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalController rentalController;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private VehicleMissionReportRepository reportRepository;

    @Test
    void testReturnRequiresOdometerAndSavesReports() throws Exception {
        Employee emp = employeeRepository.getAllEmployees().stream()
                .filter(e -> !e.isRenting())
                .findFirst().orElse(null);
        Vehicle veh = vehicleRepository.listAvailableVehicles().stream()
                .findFirst().orElse(null);
        if (emp == null || veh == null) return;

        // Pickup
        rentalService.pickup(emp.getDeviceUserId(), veh.getPlate(), "1405-01-01 10:00", "شیراز");

        VehicleRepository.VehicleRow rowBefore = vehicleRepository.findRowByPlate(veh.getPlate());
        int odoBefore = rowBefore.currentOdometer;

        // Missing odometer
        ReturnRequest reqMissingOdo = new ReturnRequest();
        reqMissingOdo.setDeviceUserId(emp.getDeviceUserId());
        assertThrows(IllegalArgumentException.class, () -> rentalController.returnCar(reqMissingOdo));

        // Lower odometer
        if (odoBefore > 0) {
            ReturnRequest reqLower = new ReturnRequest();
            reqLower.setDeviceUserId(emp.getDeviceUserId());
            reqLower.setReturnOdometer(odoBefore - 10);
            assertThrows(IllegalArgumentException.class, () -> rentalController.returnCar(reqLower));
        }

        // Valid return with fuel report
        ReturnRequest validReq = new ReturnRequest();
        validReq.setDeviceUserId(emp.getDeviceUserId());
        validReq.setReturnOdometer(odoBefore + 150);

        CreateMissionReportRequest fuelRep = new CreateMissionReportRequest();
        fuelRep.setReportType("FUEL");
        fuelRep.setTitle("بنزین مأموریت");
        fuelRep.setAmount(600000.0);
        fuelRep.setLiters(20.0);
        validReq.setReports(List.of(fuelRep));

        var res = rentalController.returnCar(validReq);
        assertTrue(res.isOk());

        // Verify vehicle odometer updated
        VehicleRepository.VehicleRow rowAfter = vehicleRepository.findRowByPlate(veh.getPlate());
        assertEquals(odoBefore + 150, rowAfter.currentOdometer);
        assertFalse(rowAfter.rented);

        // Verify report persisted
        List<VehicleMissionReport> reps = reportRepository.findByVehicleId(rowAfter.id);
        assertTrue(reps.stream().anyMatch(r -> "FUEL".equals(r.getReportType()) && r.getAmount() == 600000.0));
    }
}
