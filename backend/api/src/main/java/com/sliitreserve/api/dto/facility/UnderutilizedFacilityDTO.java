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
public class UnderutilizedFacilityDTO {

    private UUID facilityId;
    private String facilityName;
    private double utilizationPercentage;
    private boolean persistentForSevenDays;
    private Integer consecutiveUnderutilizedDays;
    private Facility.FacilityStatus status;
}
