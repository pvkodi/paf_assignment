package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Request DTO for creating or updating Facilities.
 * Excludes ID and timestamps; server generates these.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacilityRequestDTO extends BaseRequestDTO {

    @JsonProperty("facility_code")
    private String facilityCode;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type; // LECTURE_HALL, LAB, MEETING_ROOM, AUDITORIUM, EQUIPMENT, SPORTS_FACILITY

    @JsonProperty("capacity")
    private Integer capacity;

    @JsonProperty("location")
    private String location;

    @JsonProperty("building")
    private String building;

    @JsonProperty("floor")
    private String floor;

    @JsonProperty("status")
    private String status; // ACTIVE, OUT_OF_SERVICE

    @JsonProperty("availability_start")
    private LocalTime availabilityStart;

    @JsonProperty("availability_end")
    private LocalTime availabilityEnd;
}
