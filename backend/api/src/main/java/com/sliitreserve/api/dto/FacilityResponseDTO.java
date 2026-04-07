package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Response DTO for Facility entities.
 * Includes all facility fields with IDs and timestamps.
 * Used for API responses to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacilityResponseDTO extends BaseResponseDTO {

    @JsonProperty("facility_code")
    private String facilityCode;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("capacity")
    private Integer capacity;

    @JsonProperty("location")
    private String location;

    @JsonProperty("building")
    private String building;

    @JsonProperty("floor")
    private String floor;

    @JsonProperty("status")
    private String status;

    @JsonProperty("availability_start")
    private LocalTime availabilityStart;

    @JsonProperty("availability_end")
    private LocalTime availabilityEnd;
}
