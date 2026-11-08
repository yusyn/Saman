package com.car.rental.service;

import com.car.rental.api.dto.VehicleHistoryDto;
import com.car.rental.db.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleHistoryIdempotencyTest {

    @Test
    @DisplayName("getHistoryByPlate does not execute updates on VehicleRepository")
    void testGetHistoryDoesNotMutateDatabase() throws SQLException {
        VehicleRepository vRepo = mock(VehicleRepository.class);
        VehicleServiceRepository sRepo = mock(VehicleServiceRepository.class);
        VehicleIssueRepository iRepo = mock(VehicleIssueRepository.class);
        VehicleFineRepository fRepo = mock(VehicleFineRepository.class);
        RentalRepository rRepo = mock(RentalRepository.class);

        VehicleRepository.VehicleRow row = new VehicleRepository.VehicleRow();
        row.id = 1;
        row.name = "Pride";
        row.plate = "11B22233";
        row.color = "White";
        row.vehicleType = "CAR";
        row.rented = false;
        row.currentOdometer = 10000;
        row.lastServiceOdometer = null;
        row.lastServiceDate = null;
        row.statusFlags = "";

        when(vRepo.findRowByPlate("11B22233")).thenReturn(row);
        when(rRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(sRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(iRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(fRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());

        VehicleHistoryService service = new VehicleHistoryService(vRepo, sRepo, iRepo, fRepo, rRepo);
        VehicleHistoryDto dto = service.getHistoryByPlate("11B22233");

        // Verify updateStatusFlags is NEVER invoked during read
        verify(vRepo, never()).updateStatusFlags(anyInt(), anyString());

        // Verify badges are still computed and set on DTO
        assertNotNull(dto);
        assertTrue(dto.getStatusBadges().contains(VehicleHistoryService.BADGE_NEEDS_SERVICE));
    }

    @Test
    @DisplayName("getHistoryById does not execute updates on VehicleRepository")
    void testGetHistoryByIdDoesNotMutateDatabase() throws SQLException {
        VehicleRepository vRepo = mock(VehicleRepository.class);
        VehicleServiceRepository sRepo = mock(VehicleServiceRepository.class);
        VehicleIssueRepository iRepo = mock(VehicleIssueRepository.class);
        VehicleFineRepository fRepo = mock(VehicleFineRepository.class);
        RentalRepository rRepo = mock(RentalRepository.class);

        VehicleRepository.VehicleRow row = new VehicleRepository.VehicleRow();
        row.id = 1;
        row.name = "Pride";
        row.plate = "11B22233";
        row.color = "White";
        row.vehicleType = "CAR";
        row.rented = false;
        row.currentOdometer = 10000;
        row.lastServiceOdometer = null;
        row.lastServiceDate = null;
        row.statusFlags = "";

        when(vRepo.findRowById(1)).thenReturn(row);
        when(rRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(sRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(iRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());
        when(fRepo.findByVehicleId(1)).thenReturn(Collections.emptyList());

        VehicleHistoryService service = new VehicleHistoryService(vRepo, sRepo, iRepo, fRepo, rRepo);
        VehicleHistoryDto dto = service.getHistoryById(1);

        verify(vRepo, never()).updateStatusFlags(anyInt(), anyString());
        assertNotNull(dto);
        assertTrue(dto.getStatusBadges().contains(VehicleHistoryService.BADGE_NEEDS_SERVICE));
    }
}
