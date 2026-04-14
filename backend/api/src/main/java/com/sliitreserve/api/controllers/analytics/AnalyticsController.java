package com.sliitreserve.api.controllers.analytics;

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
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Slf4j
public class AnalyticsController {

    @Autowired
    private UtilizationAnalyticsService analyticsService;

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
            if (from.isAfter(to)) {
                log.warn("Invalid date range: from {} is after to {}", from, to);
                return ResponseEntity.badRequest().build();
            }

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
