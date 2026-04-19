package com.sliitreserve.api.dto.facility;

import com.sliitreserve.api.entities.facility.Facility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating or updating Facilities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacilityRequestDTO extends com.sliitreserve.api.dto.BaseRequestDTO {

    private String facilityCode;

    @NotBlank(message = "Facility name is required")
    private String name;

    @NotNull(message = "Facility type is required")
    private Facility.FacilityType type;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be greater than 0")
    private Integer capacity;

    @NotBlank(message = "Building is required")
    private String building;

    private String floor;

    @NotBlank(message = "Location description is required")
    private String locationDescription;

    @NotNull(message = "Availability start time is required")
    private LocalTime availabilityStartTime;

    @NotNull(message = "Availability end time is required")
    private LocalTime availabilityEndTime;

    private Facility.FacilityStatus status;

    /**
     * Multi-window availability schedule (Mon–Sun, multiple windows per day).
     * Each entry specifies a day of week plus start and end times.
     * A facility may have multiple entries for the same day (split windows).
     */
    private List<AvailabilityWindowDTO> availabilityWindows;

    /**
     * Geofencing fields for location-based check-in verification
     */
    private Double latitude;
    private Double longitude;
    private String wifiSSID;
    private String wifiMacAddress;
    private Integer geofenceRadiusMeters;

    /**
     * Subtype-specific attributes (composition extension point).
     */
    private Map<String, Object> subtypeAttributes;
}
