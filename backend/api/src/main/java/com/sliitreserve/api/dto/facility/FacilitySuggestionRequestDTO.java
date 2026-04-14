package com.sliitreserve.api.dto.facility;

import com.sliitreserve.api.entities.facility.Facility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilitySuggestionRequestDTO {

    @NotNull(message = "Facility type is required")
    private Facility.FacilityType type;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be greater than 0")
    private Integer capacity;

    @NotNull(message = "Start time is required")
    private LocalDateTime start;

    @NotNull(message = "End time is required")
    private LocalDateTime end;

    private String preferredBuilding;
}
