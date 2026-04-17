package com.sliitreserve.api.dto.facility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityUtilizationDTO {

    private UUID facilityId;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private double totalAvailableHours;
    private double totalBookedHours;
    private double utilizationPercentage;
}
