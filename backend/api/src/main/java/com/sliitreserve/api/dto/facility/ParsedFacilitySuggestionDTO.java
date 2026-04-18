package com.sliitreserve.api.dto.facility;

import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedFacilitySuggestionDTO {
    private String facilityCode;
    private String name;
    private FacilityType type;
    private Integer capacity;
    private String building;
    private String floor;
}
