package com.sliitreserve.api.unit.analytics;

import com.sliitreserve.api.dto.analytics.UtilizationResponse;
import com.sliitreserve.api.dto.integration.BookingDTO;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.services.analytics.RecommendationService;
import com.sliitreserve.api.services.analytics.UtilizationAnalyticsService;
import com.sliitreserve.api.services.integration.BookingIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilizationServiceTest {

    @Mock
    private UtilizationSnapshotRepository snapshotRepository;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private BookingIntegrationService bookingIntegrationService;

    @InjectMocks
    private UtilizationAnalyticsService utilizationAnalyticsService;

    @Test
    void generateUtilizationAnalytics_returnsEmptyResponse_whenNoSnapshotsFound() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);

        when(snapshotRepository.findBySnapshotDateBetweenOrderBySnapshotDateAsc(from, to))
            .thenReturn(List.of());

        UtilizationResponse response = utilizationAnalyticsService.generateUtilizationAnalytics(from, to);

        assertNotNull(response);
        assertTrue(response.getHeatmap().isEmpty());
        assertTrue(response.getUnderutilizedFacilities().isEmpty());
        assertTrue(response.getRecommendations().isEmpty());
    }

    @Test
    void generateUtilizationAnalytics_filtersIneligibleFacilities_andBuildsInsights() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 2);

        Facility eligibleFacility = facility(UUID.randomUUID(), "Eligible Hall", FacilityStatus.ACTIVE);
        Facility lowHoursFacility = facility(UUID.randomUUID(), "Low Hours Lab", FacilityStatus.ACTIVE);
        Facility inactiveFacility = facility(UUID.randomUUID(), "Inactive Hall", FacilityStatus.OUT_OF_SERVICE);

        List<UtilizationSnapshot> snapshots = List.of(
            snapshot(eligibleFacility, LocalDate.of(2026, 4, 1), "30.00", "6.00", "20.00", false, 0),
            snapshot(eligibleFacility, LocalDate.of(2026, 4, 2), "30.00", "3.00", "10.00", true, 8),
            snapshot(lowHoursFacility, LocalDate.of(2026, 4, 1), "40.00", "8.00", "20.00", true, 3),
            snapshot(inactiveFacility, LocalDate.of(2026, 4, 1), "100.00", "25.00", "25.00", true, 9)
        );

        UtilizationResponse.RecommendedAlternative recommendation =
            UtilizationResponse.RecommendedAlternative.builder()
                .requestedFacilityId(eligibleFacility.getId())
                .alternativeFacilityId(UUID.randomUUID())
                .alternativeFacilityName("Alternative Hall")
                .capacity(120)
                .utilizationPercent(new BigDecimal("55.00"))
                .reason("Better utilization")
                .build();

        BookingDTO booking1 = BookingDTO.builder()
            .facilityId(eligibleFacility.getId())
            .startDateTime(from.atTime(9, 0))
            .endDateTime(from.atTime(11, 0))
            .build();
        BookingDTO booking2 = BookingDTO.builder()
            .facilityId(eligibleFacility.getId())
            .startDateTime(from.atTime(14, 0))
            .endDateTime(from.atTime(15, 0))
            .build();

        when(snapshotRepository.findBySnapshotDateBetweenOrderBySnapshotDateAsc(from, to))
            .thenReturn(snapshots);
        when(bookingIntegrationService.getBookingsForFacility(eq(eligibleFacility.getId()), any(), any()))
            .thenReturn(List.of(booking1, booking2));
        when(recommendationService.generateRecommendations(anyList()))
            .thenReturn(List.of(recommendation));

        UtilizationResponse response = utilizationAnalyticsService.generateUtilizationAnalytics(from, to);

        // Heatmap should have entries for 9-10h, 10-11h, and 14-15h for the first day
        assertTrue(response.getHeatmap().size() >= 2);
        assertEquals(1, response.getUnderutilizedFacilities().size());
        assertEquals(eligibleFacility.getId(), response.getUnderutilizedFacilities().get(0).getFacilityId());
        assertEquals(1, response.getRecommendations().size());

        verify(recommendationService).generateRecommendations(anyList());
    }

    @Test
    void generateUtilizationAnalytics_gracefullyHandlesRecommendationServiceFailure() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 1);

        Facility facility = facility(UUID.randomUUID(), "Physics Lab", FacilityStatus.ACTIVE);
        List<UtilizationSnapshot> snapshots = List.of(
            snapshot(facility, LocalDate.of(2026, 4, 1), "60.00", "6.00", "10.00", true, 2)
        );

        when(snapshotRepository.findBySnapshotDateBetweenOrderBySnapshotDateAsc(from, to))
            .thenReturn(snapshots);
        when(bookingIntegrationService.getBookingsForFacility(any(), any(), any()))
            .thenReturn(List.of());
        when(recommendationService.generateRecommendations(anyList()))
            .thenThrow(new RuntimeException("Recommendation engine unavailable"));

        UtilizationResponse response = utilizationAnalyticsService.generateUtilizationAnalytics(from, to);

        assertEquals(1, response.getUnderutilizedFacilities().size());
        assertTrue(response.getRecommendations().isEmpty());
    }

    private Facility facility(UUID id, String name, FacilityStatus status) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName(name);
        facility.setStatus(status);
        return facility;
    }

    private UtilizationSnapshot snapshot(
        Facility facility,
        LocalDate date,
        String availableHours,
        String bookedHours,
        String utilizationPercent,
        boolean underutilized,
        int consecutiveDays
    ) {
        return UtilizationSnapshot.builder()
            .facility(facility)
            .snapshotDate(date)
            .availableHours(new BigDecimal(availableHours))
            .bookedHours(new BigDecimal(bookedHours))
            .utilizationPercent(new BigDecimal(utilizationPercent))
            .underutilized(underutilized)
            .consecutiveUnderutilizedDays(consecutiveDays)
            .build();
    }
}
