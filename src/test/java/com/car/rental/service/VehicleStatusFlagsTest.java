package com.car.rental.service;

import com.car.rental.db.*;
import com.car.rental.model.VehicleDocument;
import com.car.rental.model.VehicleMissionReport;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleStatusFlagsTest {

    @Test
    void testDocumentExpiryAndMissionReportBadges() throws SQLException {
        VehicleRepository vRepo = mock(VehicleRepository.class);
        VehicleServiceRepository sRepo = mock(VehicleServiceRepository.class);
        VehicleIssueRepository iRepo = mock(VehicleIssueRepository.class);
        VehicleFineRepository fRepo = mock(VehicleFineRepository.class);
        RentalRepository rRepo = mock(RentalRepository.class);
        VehicleMissionReportRepository mrRepo = mock(VehicleMissionReportRepository.class);
        VehicleDocumentRepository vdRepo = mock(VehicleDocumentRepository.class);
        EmployeeRepository eRepo = mock(EmployeeRepository.class);

        VehicleRepository.VehicleRow row = new VehicleRepository.VehicleRow();
        row.id = 10;
        row.name = "Peugeot";
        row.plate = "22B33344";
        row.currentOdometer = 25000;

        when(iRepo.hasOpenHighSeverity(10)).thenReturn(false);
        when(iRepo.hasInProgress(10)).thenReturn(false);
        when(fRepo.hasUnpaid(10)).thenReturn(false);

        // Mission report with unpaid fine
        when(mrRepo.hasUnpaidFine(10)).thenReturn(true);
        when(mrRepo.hasOpenAccident(10)).thenReturn(false);

        // Vehicle document expired (e.g. 1400-01-01)
        VehicleDocument doc = new VehicleDocument(
                1, 10, "THIRD_PARTY_INSURANCE", "12345",
                "1399-01-01", "1400-01-01", null, null, null
        );
        when(vdRepo.findByVehicleId(10)).thenReturn(List.of(doc));

        VehicleHistoryService service = new VehicleHistoryService(vRepo, sRepo, iRepo, fRepo, rRepo, mrRepo, vdRepo, eRepo);
        List<String> badges = service.computeStatusBadges(row, Collections.emptyList(), 10);

        assertTrue(badges.contains(VehicleHistoryService.BADGE_HAS_FINE));
        assertTrue(badges.contains(VehicleHistoryService.BADGE_INSURANCE_EXPIRED));
        assertFalse(badges.contains("NEEDS_SERVICE"));
    }
}
