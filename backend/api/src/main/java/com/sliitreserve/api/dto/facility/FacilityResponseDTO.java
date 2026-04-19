package com.sliitreserve.api.dto.facility;

import com.sliitreserve.api.entities.facility.Facility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
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
    /**
     * Multi-window availability schedule (Mon–Sun, multiple windows per day).
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
    
    private Map<String, Object> subtypeAttributes;
}
