package com.sliitreserve.api.services.analytics;

import com.sliitreserve.api.dto.analytics.UtilizationResponse;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
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

            List<UtilizationResponse.HeatmapEntry> heatmap = buildHeatmap(snapshots);
            List<UtilizationResponse.UnderutilizedFacility> underutilized = identifyUnderutilized(snapshots);
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
        return snapshots.stream()
            .filter(UtilizationSnapshot::isUnderutilized)
            .map(snapshot -> {
                String recommendation = snapshot.hasPersistentUnderutilization()
                    ? "CRITICAL: Underutilized for " + snapshot.getConsecutiveUnderutilizedDays() + " days. Consider removing from schedule or repurposing."
                    : "Monitor: Utilization trending down. Consider promotional strategies.";

                return UtilizationResponse.UnderutilizedFacility.builder()
                    .facilityId(snapshot.getFacility().getId())
                    .facilityName(snapshot.getFacility().getName())
                    .utilizationPercent(snapshot.getUtilizationPercent())
                    .consecutiveUnderutilizedDays(snapshot.getConsecutiveUnderutilizedDays())
                    .recommendation(recommendation)
                    .build();
            })
            .collect(Collectors.toList());
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
}
