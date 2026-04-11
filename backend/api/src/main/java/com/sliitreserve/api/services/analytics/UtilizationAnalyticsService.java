package com.sliitreserve.api.services.analytics;

import com.sliitreserve.api.dto.analytics.UtilizationResponse;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.repositories.UtilizationSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilization Analytics Service - Analytics calculations and reporting.
 *
 * Purpose: Provides admin analytics for facility utilization, underutilization detection,
 * and alternative facility recommendations.
 *
 * Design:
 * - Queries utilization snapshots from database
 * - Calculates heatmaps by day-of-week and hour-of-day
 * - Identifies underutilized facilities (< 30% average)
 * - Generates alternative facility recommendations
 *
 * Integration:
 * - Used by AnalyticsController for GET /analytics/utilization
 * - Depends on UtilizationSnapshotService to populate snapshots (T080)
 * - Depends on RecommendationService for suggestions (T081)
 */
@Service
@Slf4j
public class UtilizationAnalyticsService {

    private static final BigDecimal MIN_ANALYSIS_AVAILABLE_HOURS = new BigDecimal("50.00");

    @Autowired
    private UtilizationSnapshotRepository snapshotRepository;

    @Autowired(required = false)
    private RecommendationService recommendationService;

    /**
     * Generate utilization analytics for date range.
     *
     * Steps:
     * 1. Query snapshots for date range
     * 2. Build heatmap by facility and time
     * 3. Identify underutilized facilities
     * 4. Generate alternative recommendations
     *
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return UtilizationResponse with heatmap and insights
     */
    public UtilizationResponse generateUtilizationAnalytics(LocalDate fromDate, LocalDate toDate) {
        log.info("Generating utilization analytics for period {} to {}", fromDate, toDate);

        try {
            List<UtilizationSnapshot> snapshots = snapshotRepository.findBySnapshotDateBetweenOrderBySnapshotDateAsc(fromDate, toDate);

            if (snapshots.isEmpty()) {
                log.warn("No utilization snapshots found for period {} to {}", fromDate, toDate);
                return UtilizationResponse.builder()
                    .heatmap(new ArrayList<>())
                    .underutilizedFacilities(new ArrayList<>())
                    .recommendations(new ArrayList<>())
                    .build();
            }

            List<UtilizationSnapshot> eligibleSnapshots = filterEligibleSnapshots(snapshots);
            if (eligibleSnapshots.isEmpty()) {
                return UtilizationResponse.builder()
                    .heatmap(new ArrayList<>())
                    .underutilizedFacilities(new ArrayList<>())
                    .recommendations(new ArrayList<>())
                    .build();
            }

            List<UtilizationResponse.HeatmapEntry> heatmap = buildHeatmap(eligibleSnapshots);
            List<UtilizationResponse.UnderutilizedFacility> underutilized = identifyUnderutilized(eligibleSnapshots);
            List<UtilizationResponse.RecommendedAlternative> recommendations = generateRecommendations(underutilized);

            return UtilizationResponse.builder()
                .heatmap(heatmap)
                .underutilizedFacilities(underutilized)
                .recommendations(recommendations)
                .build();
        } catch (Exception e) {
            log.error("Error generating utilization analytics for period {} to {}", fromDate, toDate, e);
            throw new RuntimeException("Failed to generate utilization analytics", e);
        }
    }

    private List<UtilizationSnapshot> filterEligibleSnapshots(List<UtilizationSnapshot> snapshots) {
        Map<UUID, BigDecimal> availableHoursByFacility = snapshots.stream()
            .filter(snapshot -> snapshot.getFacility() != null)
            .filter(snapshot -> snapshot.getFacility().getStatus() == FacilityStatus.ACTIVE)
            .collect(
                Collectors.groupingBy(
                    snapshot -> snapshot.getFacility().getId(),
                    Collectors.reducing(
                        BigDecimal.ZERO,
                        snapshot -> zeroIfNull(snapshot.getAvailableHours()),
                        BigDecimal::add
                    )
                )
            );

        Set<UUID> eligibleFacilityIds = availableHoursByFacility.entrySet().stream()
            .filter(entry -> entry.getValue().compareTo(MIN_ANALYSIS_AVAILABLE_HOURS) >= 0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        return snapshots.stream()
            .filter(snapshot -> snapshot.getFacility() != null)
            .filter(snapshot -> eligibleFacilityIds.contains(snapshot.getFacility().getId()))
            .collect(Collectors.toList());
    }

    /**
     * Build heatmap from snapshots.
     *
     * Groups by facility and calculates average utilization by day-of-week and hour-of-day.
     * (Simplified: returns daily aggregates, assumes hourly data would come from extended snapshots)
     *
     * @param snapshots List of utilization snapshots
     * @return List of heatmap entries
     */
    private List<UtilizationResponse.HeatmapEntry> buildHeatmap(List<UtilizationSnapshot> snapshots) {
        List<UtilizationResponse.HeatmapEntry> heatmap = new ArrayList<>();

        snapshots.forEach(snapshot -> {
            // Map LocalDate to day-of-week (0=Monday, 6=Sunday)
            int dayOfWeek = snapshot.getSnapshotDate().getDayOfWeek().getValue() - 1; // Convert to 0-6

            UtilizationResponse.HeatmapEntry entry = UtilizationResponse.HeatmapEntry.builder()
                .facilityId(snapshot.getFacility().getId())
                .facilityName(snapshot.getFacility().getName())
                .dayOfWeek(dayOfWeek)
                .hourOfDay(0) // TODO: Extended to hourly snapshots in future
                .utilizationPercent(snapshot.getUtilizationPercent())
                .build();

            heatmap.add(entry);
        });

        return heatmap;
    }

    /**
     * Identify underutilized facilities (< 30% average utilization).
     *
     * From FR-038: "Mark facilities underutilized when average utilization is below 30%
     * over 30 days and flag if persistent for more than 7 consecutive days."
     *
     * @param snapshots List of utilization snapshots
     * @return List of underutilized facilities
     */
    private List<UtilizationResponse.UnderutilizedFacility> identifyUnderutilized(List<UtilizationSnapshot> snapshots) {
        Map<UUID, List<UtilizationSnapshot>> byFacility = snapshots.stream()
            .filter(snapshot -> snapshot.getFacility() != null)
            .collect(Collectors.groupingBy(snapshot -> snapshot.getFacility().getId()));

        List<UtilizationResponse.UnderutilizedFacility> underutilizedFacilities = new ArrayList<>();

        for (List<UtilizationSnapshot> facilitySnapshots : byFacility.values()) {
            UtilizationSnapshot latestSnapshot = facilitySnapshots.stream()
                .max(Comparator.comparing(UtilizationSnapshot::getSnapshotDate))
                .orElse(null);

            if (latestSnapshot == null || !latestSnapshot.isUnderutilized()) {
                continue;
            }

            String recommendation = latestSnapshot.hasPersistentUnderutilization()
                ? "CRITICAL: Underutilized for " + latestSnapshot.getConsecutiveUnderutilizedDays() + " days. Consider repurposing or schedule changes."
                : "Monitor: Utilization trending down. Consider promotional strategies.";

            underutilizedFacilities.add(
                UtilizationResponse.UnderutilizedFacility.builder()
                    .facilityId(latestSnapshot.getFacility().getId())
                    .facilityName(latestSnapshot.getFacility().getName())
                    .utilizationPercent(latestSnapshot.getUtilizationPercent())
                    .consecutiveUnderutilizedDays(latestSnapshot.getConsecutiveUnderutilizedDays())
                    .recommendation(recommendation)
                    .build()
            );
        }

        underutilizedFacilities.sort(
            Comparator.comparing(facility -> zeroIfNull(facility.getUtilizationPercent()))
        );

        return underutilizedFacilities;
    }

    /**
     * Generate alternative facility recommendations.
     *
     * Delegates to RecommendationService if available (T081).
     * Otherwise returns empty list.
     *
     * @param underutilized List of underutilized facilities
     * @return List of recommendations
     */
    private List<UtilizationResponse.RecommendedAlternative> generateRecommendations(List<UtilizationResponse.UnderutilizedFacility> underutilized) {
        if (recommendationService == null) {
            log.debug("RecommendationService not available (T081 not implemented yet)");
            return new ArrayList<>();
        }

        try {
            return recommendationService.generateRecommendations(underutilized);
        } catch (Exception e) {
            log.warn("Failed to generate recommendations", e);
            return new ArrayList<>();
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
