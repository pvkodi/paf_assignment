package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.booking.CheckInMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for check-in operations with geofencing support.
 * 
 * Supports both QR and manual check-in methods with GPS-based geofencing verification.
 * 
 * Geofencing fields (GPS-only):
 * - latitude: User's current GPS latitude (required for location verification)
 * - longitude: User's current GPS longitude (required for location verification)
 * 
 * Note: WiFi detection is not available in standard web browsers, so geofencing uses GPS only.
 * 
 * Check-in will only succeed if:
 * 1. User is within GPS radius of facility
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequestDTO {

    @NotNull(message = "Check-in method is required (QR or MANUAL)")
    @JsonProperty("method")
    private CheckInMethod method;

    @JsonProperty("notes")
    private String notes;

    @NotNull(message = "Latitude is required for GPS geofencing verification")
    @JsonProperty("latitude")
    private Double latitude;  // User's current GPS latitude

    @NotNull(message = "Longitude is required for GPS geofencing verification")
    @JsonProperty("longitude")
    private Double longitude;  // User's current GPS longitude
}

