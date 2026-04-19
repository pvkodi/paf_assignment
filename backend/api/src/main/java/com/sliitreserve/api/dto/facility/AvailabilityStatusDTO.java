package com.sliitreserve.api.dto.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for facility availability status insights
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityStatusDTO {
    
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
     * Current status: FREE, BUSY, FULL
     */
    @JsonProperty("current_status")
    private String currentStatus;
    
    @JsonProperty("spots_available")
    private Integer spotsAvailable;
    
    /**
     * Minutes until the facility becomes free (or changes status)
     */
    @JsonProperty("minutes_until_free")
    private Long minutesUntilFree;
    
    @JsonProperty("next_booking_time")
    private String nextBookingTime;
    
    /**
     * 30-day average utilization percentage (0-100)
     */
    @JsonProperty("avg_utilization_30day")
    private Integer avgUtilization30day;
    
    /**
     * 7-day average utilization percentage (0-100)
     */
    @JsonProperty("avg_utilization_7day")
    private Integer avgUtilization7day;
    
    /**
     * Trend direction: "UP" (getting busier), "DOWN" (getting quieter), "STABLE"
     */
    @JsonProperty("trend_direction")
    private String trendDirection;
    
    /**
     * Percentage change from 30-day to 7-day average
     */
    @JsonProperty("trend_percent_change")
    private Double trendPercentChange;
    
    /**
     * Best time recommendations for booking
     */
    @JsonProperty("best_booking_slots")
    private java.util.List<BookingSlotDTO> bestBookingSlots;
}
