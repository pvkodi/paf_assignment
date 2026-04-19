package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Summary DTO for Facility information in booking contexts.
 * Contains essential facility details needed for booking display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilitySummaryDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("capacity")
    private Integer capacity;

    @JsonProperty("building")
    private String building;

    @JsonProperty("floor")
    private String floor;

    @JsonProperty("location")
    private String location;

    @JsonProperty("status")
    private String status;
}
