package com.sliitreserve.api.dto.facility;

import com.sliitreserve.api.entities.facility.Facility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilitySuggestionDTO {

    private UUID facilityId;
    private String name;
    private Facility.FacilityType type;
    private Integer capacity;
    private String building;
    private Facility.FacilityStatus status;
    private boolean operational;
    private int capacityDelta;
    private double utilizationScore;
    private String timetableStatus;
}
