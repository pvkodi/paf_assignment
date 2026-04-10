package com.sliitreserve.api.services.analytics;

import com.sliitreserve.api.dto.analytics.UtilizationResponse;

import java.util.List;

/**
 * Recommendation Service - Alternative facility suggestions.
 *
 * Purpose: Generate alternative facility recommendations for underutilized slots.
 *
 * Responsibilities:
 * - Suggest alternative facilities for requested time slots
 * - Prioritize by capacity fit, facility type, location
 * - Provide reasoning for recommendations
 *
 * Implementation: T081 (not yet implemented)
 * Contract: Used by UtilizationAnalyticsService
 */
public interface RecommendationService {

    /**
     * Generate alternative facility recommendations for underutilized facilities.
     *
     * From FR-039: "System MUST suggest alternative facilities for requested time slots
     * based on availability and capacity fit."
     *
     * @param underutilized List of underutilized facilities
     * @return List of recommendations
     */
    List<UtilizationResponse.RecommendedAlternative> generateRecommendations(
        List<UtilizationResponse.UnderutilizedFacility> underutilized
    );
}
