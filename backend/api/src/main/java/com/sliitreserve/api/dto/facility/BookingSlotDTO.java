package com.sliitreserve.api.dto.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for booking slot recommendation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSlotDTO {
    
    @JsonProperty("day_of_week")
    private String dayOfWeek;
    
    @JsonProperty("time_slot")
    private String timeSlot; // "10:00-12:00"
    
    @JsonProperty("availability_percent")
    private Integer availabilityPercent;
    
    @JsonProperty("reason")
    private String reason; // "Typically free at this time"
}
