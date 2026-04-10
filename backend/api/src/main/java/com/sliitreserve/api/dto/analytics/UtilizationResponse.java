package com.sliitreserve.api.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * UtilizationResponse DTO - Admin analytics response with heatmap and insights.
 *
 * Contains:
 * - Heatmap: Utilization % by facility, day-of-week, and hour-of-day
 * - Underutilized facilities: Facilities below 30% utilization threshold
 * - Alternative facility suggestions: Recommendations for better utilization
 *
 * Used by AnalyticsController GET /analytics/utilization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilizationResponse {

    private List<HeatmapEntry> heatmap;

    private List<UnderutilizedFacility> underutilizedFacilities;

    private List<RecommendedAlternative> recommendations;

    /**
     * Heatmap Entry - Utilization for facility at specific time (day, hour).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeatmapEntry {
        private UUID facilityId;
        private String facilityName;
        private Integer dayOfWeek; // 0=Monday, 6=Sunday
        private Integer hourOfDay; // 0-23
        private BigDecimal utilizationPercent; // 0-100+
    }

    /**
     * Underutilized Facility - Facilities below 30% average utilization.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnderutilizedFacility {
        private UUID facilityId;
        private String facilityName;
        private BigDecimal utilizationPercent;
        private Integer consecutiveUnderutilizedDays;
        private String recommendation;
    }

    /**
     * Recommended Alternative - Better utilized facility for time slot.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedAlternative {
        private UUID requestedFacilityId;
        private UUID alternativeFacilityId;
        private String alternativeFacilityName;
        private Integer capacity;
        private BigDecimal utilizationPercent;
        private String reason; // e.g., "30% more availability", "same facility type"
    }
}
