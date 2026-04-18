package com.sliitreserve.api.services.analytics;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.services.facility.FacilityTimetableService;
import com.sliitreserve.api.services.integration.BookingIntegrationService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealTimeAnalyticsService {

    private final FacilityRepository facilityRepository;
    private final FacilityTimetableService timetableService;
    private final BookingIntegrationService bookingIntegrationService;

    @Data
    @Builder
    public static class RealTimeStatus {
        private int totalFacilities;
        private int occupiedNow;
        private int vacantNow;
        private int maintenanceCount;
        private double occupancyRate;
    }

    public RealTimeStatus getCurrentCampusStatus() {
        List<Facility> allFacilities = facilityRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime().withMinute(0).withSecond(0).withNano(0);
        
        int total = allFacilities.size();
        int maintenance = (int) allFacilities.stream()
                .filter(f -> f.getStatus() == FacilityStatus.OUT_OF_SERVICE)
                .count();

        List<Facility> activeFacilities = allFacilities.stream()
                .filter(f -> f.getStatus() == FacilityStatus.ACTIVE)
                .collect(Collectors.toList());

        int occupiedCount = 0;
        for (Facility f : activeFacilities) {
            boolean isOccupiedByClass = timetableService.isOccupied(f.getFacilityCode(), now.getDayOfWeek(), currentTime);
            
            // Check if occupied by booking (fetch for current hour)
            boolean isOccupiedByBooking = bookingIntegrationService.getBookingsForFacility(
                    f.getId(), now.withMinute(0), now.withMinute(59)).size() > 0;
            
            if (isOccupiedByClass || isOccupiedByBooking) {
                occupiedCount++;
            }
        }

        return RealTimeStatus.builder()
                .totalFacilities(total)
                .occupiedNow(occupiedCount)
                .vacantNow(activeFacilities.size() - occupiedCount)
                .maintenanceCount(maintenance)
                .occupancyRate(total > 0 ? (double) occupiedCount / total * 100 : 0)
                .build();
    }
}
