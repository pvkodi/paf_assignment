package com.sliitreserve.api.controllers;

import com.sliitreserve.api.dto.analytics.UtilizationResponse;
import com.sliitreserve.api.services.analytics.UtilizationAnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Analytics Controller - Admin utilization analytics and insights.
 *
 * Endpoints:
 * - GET /analytics/utilization: Utilization heatmap and underutilization insights
 *
 * RBAC: All endpoints require ADMIN role (per FR-036, FR-082).
 * Response: UtilizationResponse with heatmap, underutilized facilities, and recommendations.
 *
 * Integration:
 * - Depends on UtilizationSnapshotService (T080) to populate daily snapshots
 * - Depends on RecommendationService (T081) for alternative suggestions
 * - Called by frontend analytics dashboard (T083)
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Slf4j
public class AnalyticsController {

    @Autowired
    private UtilizationAnalyticsService analyticsService;

    /**
     * Get utilization analytics for date range with heatmap and insights.
     *
     * From FR-036: "System MUST provide admin-only utilization analytics using booked-hours
     * to available-hours percentage."
     *
     * From FR-037: "System MUST exclude facilities under maintenance and those with less
     * than 50 available hours in the analysis window."
     *
     * From FR-038: "System MUST mark facilities underutilized when average utilization is
     * below 30% over 30 days and flag if persistent for more than 7 consecutive days."
     *
     * Query Parameters:
     * - from: Start date (required, format: YYYY-MM-DD)
     * - to: End date (required, format: YYYY-MM-DD)
     *
     * Response:
     * - heatmap: [ { facilityId, facilityName, dayOfWeek, hourOfDay, utilizationPercent } ]
     * - underutilizedFacilities: [ { facilityId, utilizationPercent, consecutiveDays, recommendation } ]
     * - recommendations: [ { requestedFacilityId, alternativeFacilityId, reason } ]
     *
     * @param from Start date (query param, format: YYYY-MM-DD)
     * @param to End date (query param, format: YYYY-MM-DD)
     * @return UtilizationResponse with analytics data
     */
    @GetMapping("/utilization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilizationResponse> getUtilizationAnalytics(
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
    ) {
        log.info("Analytics request: utilization for period {} to {}", from, to);

        try {
            // Validate date range
            if (from.isAfter(to)) {
                log.warn("Invalid date range: from {} is after to {}", from, to);
                return ResponseEntity.badRequest().build();
            }

            // Validate maximum range (e.g., 90 days)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(from, to);
            if (daysBetween > 90) {
                log.warn("Date range exceeds 90 days: {} days requested", daysBetween);
                return ResponseEntity.badRequest().build();
            }

            UtilizationResponse analytics = analyticsService.generateUtilizationAnalytics(from, to);

            log.info("Generated analytics for {} to {}: {} heatmap entries, {} underutilized facilities",
                from, to, analytics.getHeatmap().size(), analytics.getUnderutilizedFacilities().size());

            return ResponseEntity.ok(analytics);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid analytics request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating utilization analytics for period {} to {}", from, to, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
