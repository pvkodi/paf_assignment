package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.dto.facility.FacilitySuggestionDTO;
import com.sliitreserve.api.dto.facility.FacilitySuggestionRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityUtilizationDTO;
import com.sliitreserve.api.dto.facility.UnderutilizedFacilityDTO;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.services.integration.BookingIntegrationService;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityOptimizationService {

    private static final double UNDERUTILIZATION_THRESHOLD = 30.0;
    private static final double MIN_AVAILABLE_HOURS = 50.0;

    private final FacilityRepository facilityRepository;
    private final FacilityService facilityService;
    private final BookingIntegrationService bookingIntegrationService;
    private final MaintenanceIntegrationService maintenanceIntegrationService;
    private final UtilizationSnapshotRepository utilizationSnapshotRepository;

    public FacilityUtilizationDTO getFacilityUtilization(UUID facilityId, LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility", String.valueOf(facilityId)));

        boolean underMaintenance = maintenanceIntegrationService.isFacilityUnderMaintenance(facilityId, start, end);
        double availableHours = underMaintenance ? 0.0 : calculateTotalAvailableHours(facility, start, end);
        double bookedHours = underMaintenance ? 0.0 : bookingIntegrationService.getBookedHours(facilityId, start, end);

        double utilizationPercentage = calculateUtilization(availableHours, bookedHours);

        return FacilityUtilizationDTO.builder()
                .facilityId(facilityId)
                .rangeStart(start)
                .rangeEnd(end)
                .totalAvailableHours(roundTwoDecimals(availableHours))
                .totalBookedHours(roundTwoDecimals(bookedHours))
                .utilizationPercentage(roundTwoDecimals(utilizationPercentage))
                .build();
    }

    public List<UnderutilizedFacilityDTO> getUnderutilizedFacilities(LocalDateTime endInclusive) {
        LocalDateTime normalizedEnd = endInclusive == null ? LocalDateTime.now() : endInclusive;
        LocalDateTime start = normalizedEnd.minusDays(30);

        List<Facility> activeFacilities = facilityRepository.findByStatus(Facility.FacilityStatus.ACTIVE);
        List<UnderutilizedFacilityDTO> underutilizedFacilities = new ArrayList<>();

        for (Facility facility : activeFacilities) {
            if (maintenanceIntegrationService.isFacilityUnderMaintenance(facility.getId(), start, normalizedEnd)) {
                continue;
            }

            FacilityUtilizationDTO utilization = getFacilityUtilization(facility.getId(), start, normalizedEnd);
            if (utilization.getTotalAvailableHours() < MIN_AVAILABLE_HOURS) {
                continue;
            }
            if (utilization.getUtilizationPercentage() >= UNDERUTILIZATION_THRESHOLD) {
                continue;
            }

            int consecutiveDays = resolveConsecutiveUnderutilizedDays(facility.getId());
            underutilizedFacilities.add(UnderutilizedFacilityDTO.builder()
                    .facilityId(facility.getId())
                    .facilityName(facility.getName())
                    .utilizationPercentage(utilization.getUtilizationPercentage())
                    .consecutiveUnderutilizedDays(consecutiveDays)
                    .persistentForSevenDays(consecutiveDays >= 7)
                    .status(facility.getStatus())
                    .build());
        }

        underutilizedFacilities.sort(Comparator.comparingDouble(UnderutilizedFacilityDTO::getUtilizationPercentage));
        return underutilizedFacilities;
    }

    public List<FacilitySuggestionDTO> suggestAlternativeFacilities(FacilitySuggestionRequestDTO request) {
        validateRange(request.getStart(), request.getEnd());

        List<FacilitySuggestionDTO> suggestions = new ArrayList<>();
        Set<UUID> addedFacilityIds = new HashSet<>();

        List<Facility> sameTypeFacilities = facilityService.findActiveByType(request.getType());
        collectSuggestions(sameTypeFacilities, request, suggestions, addedFacilityIds);

        if (suggestions.size() < 5) {
            List<Facility> activeFacilities = facilityRepository.findByStatus(Facility.FacilityStatus.ACTIVE);
            collectSuggestions(activeFacilities, request, suggestions, addedFacilityIds);
        }

        suggestions.sort(
                Comparator.comparingInt(FacilitySuggestionDTO::getCapacityDelta)
                        .thenComparing((left, right) -> {
                            boolean leftSameBuilding = isPreferredBuildingMatch(left.getBuilding(), request.getPreferredBuilding());
                            boolean rightSameBuilding = isPreferredBuildingMatch(right.getBuilding(), request.getPreferredBuilding());
                            return Boolean.compare(rightSameBuilding, leftSameBuilding);
                        })
        );

        return suggestions.size() > 5 ? suggestions.subList(0, 5) : suggestions;
    }

    private void collectSuggestions(
            List<Facility> candidates,
            FacilitySuggestionRequestDTO request,
            List<FacilitySuggestionDTO> suggestions,
            Set<UUID> addedFacilityIds
    ) {
        for (Facility facility : candidates) {
            if (suggestions.size() >= 5) {
                return;
            }
            if (addedFacilityIds.contains(facility.getId())) {
                continue;
            }
            if (facility.getCapacity() == null || facility.getCapacity() < request.getCapacity()) {
                continue;
            }
            if (!facilityService.isFacilityOperational(facility.getId(), request.getStart(), request.getEnd())) {
                continue;
            }

            suggestions.add(FacilitySuggestionDTO.builder()
                    .facilityId(facility.getId())
                    .name(facility.getName())
                    .type(facility.getType())
                    .capacity(facility.getCapacity())
                    .building(facility.getBuilding())
                    .status(facility.getStatus())
                    .operational(true)
                    .capacityDelta(facility.getCapacity() - request.getCapacity())
                    .build());
            addedFacilityIds.add(facility.getId());
        }
    }

    private int resolveConsecutiveUnderutilizedDays(UUID facilityId) {
        return utilizationSnapshotRepository.findFirstByFacility_IdOrderBySnapshotDateDesc(facilityId)
                .filter(UtilizationSnapshot::isUnderutilized)
                .map(snapshot -> snapshot.getConsecutiveUnderutilizedDays() == null ? 0 : snapshot.getConsecutiveUnderutilizedDays())
                .orElse(0);
    }

    private double calculateTotalAvailableHours(Facility facility, LocalDateTime start, LocalDateTime end) {
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        double totalMinutes = 0.0;

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime facilityWindowStart = LocalDateTime.of(currentDate, facility.getAvailabilityStartTime());
            LocalDateTime facilityWindowEnd = LocalDateTime.of(currentDate, facility.getAvailabilityEndTime());

            LocalDateTime effectiveStart = maxDateTime(start, facilityWindowStart);
            LocalDateTime effectiveEnd = minDateTime(end, facilityWindowEnd);

            if (effectiveEnd.isAfter(effectiveStart)) {
                totalMinutes += Duration.between(effectiveStart, effectiveEnd).toMinutes();
            }

            currentDate = currentDate.plusDays(1);
        }

        return totalMinutes / 60.0;
    }

    private LocalDateTime maxDateTime(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDateTime minDateTime(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }

    private boolean isPreferredBuildingMatch(String building, String preferredBuilding) {
        if (preferredBuilding == null || preferredBuilding.isBlank()) {
            return false;
        }
        return building != null && building.equalsIgnoreCase(preferredBuilding.trim());
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new ValidationException("Invalid time range");
        }

        if (Duration.between(start, end).toDays() > 90) {
            throw new ValidationException("Time range cannot exceed 90 days");
        }
    }

    private double calculateUtilization(double availableHours, double bookedHours) {
        if (availableHours <= 0.0) {
            return 0.0;
        }
        return (bookedHours / availableHours) * 100.0;
    }

    private double roundTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
