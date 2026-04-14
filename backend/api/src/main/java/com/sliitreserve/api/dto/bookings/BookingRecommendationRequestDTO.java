package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO for requesting facility booking recommendations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRecommendationRequestDTO {
    
    @JsonProperty("facility_type")
    private String facilityType; // e.g., "LECTURE_HALL", "LAB", "MEETING_ROOM"
    
    @JsonProperty("capacity")
    private Integer capacity;
    
    @JsonProperty("start_time")
    private LocalDateTime startTime;
    
    @JsonProperty("end_time")
    private LocalDateTime endTime;
    
    @JsonProperty("duration_hours")
    private Integer durationHours;
    
    @JsonProperty("preferred_building")
    private String preferredBuilding; // "A", "B", "C", etc.
    
    @JsonProperty("max_recommendations")
    private Integer maxRecommendations; // Default: 5
}
