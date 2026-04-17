package com.sliitreserve.api.dto.facility;

import com.sliitreserve.api.entities.facility.Facility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Map;

/**
 * Response DTO for facility APIs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacilityResponseDTO extends com.sliitreserve.api.dto.BaseResponseDTO {

    private String facilityCode;
    private String name;
    private Facility.FacilityType type;
    private Integer capacity;
    private String building;
    private String floor;
    private String locationDescription;
    private Facility.FacilityStatus status;
    private LocalTime availabilityStartTime;
    private LocalTime availabilityEndTime;
    private Map<String, Object> subtypeAttributes;
}
