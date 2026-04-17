package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for facility booking recommendation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDTO {
    
    @JsonProperty("facility_id")
    private String facilityId;
    
    @JsonProperty("facility_name")
    private String facilityName;
    
    @JsonProperty("facility_type")
    private String facilityType;
    
    @JsonProperty("building")
    private String building;
    
    @JsonProperty("capacity")
    private Integer capacity;
    
    /**
     * Overall recommendation score (0-100)
     */
    @JsonProperty("recommendation_score")
    private Integer recommendationScore;
    
    /**
     * Current availability at requested time: FREE, BUSY, FULL
     */
    @JsonProperty("availability_at_time")
    private String availabilityAtTime;
    
    /**
     * Utilization percentage at requested time (0-100)
     */
    @JsonProperty("utilization_percent_at_time")
    private Integer utilizationPercentAtTime;
    
    /**
     * List of reasons why this facility is recommended
     */
    @JsonProperty("reasons")
    private List<String> reasons;
    
    /**
     * Alternative time slots if not available at requested time
     */
    @JsonProperty("alternative_slots")
    private List<String> alternativeSlots;
    
    /**
     * Ranking position (1 = best recommendation)
     */
    @JsonProperty("rank")
    private Integer rank;
}
