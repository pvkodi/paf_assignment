package com.sliitreserve.api.dto.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Set;

/**
 * DTO for timetable availability query response.
 * Shows occupied and available time slots for a facility on a specific day.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableAvailabilityDTO {

    @JsonProperty("facility_code")
    private String facilityCode;

    @JsonProperty("facility_name")
    private String facilityName;

    @JsonProperty("day")
    private DayOfWeek day;

    @JsonProperty("occupied_slots")
    private Set<String> occupiedSlots;

    @JsonProperty("available_slots")
    private Set<String> availableSlots;

    @JsonProperty("total_available_count")
    private Integer totalAvailableCount;

    @JsonProperty("total_occupied_count")
    private Integer totalOccupiedCount;

    @JsonProperty("timetable_loaded")
    private Boolean timetableLoaded;
}
