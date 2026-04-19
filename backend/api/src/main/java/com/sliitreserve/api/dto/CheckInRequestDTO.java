package com.sliitreserve.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for check-in operations with geofencing.
 * 
 * Supports two check-in methods:
 * 1. QR: User scans QR code (automatic or manual)
 * 2. MANUAL: Staff member records check-in manually
 * 
 * Both methods require geofencing verification (WiFi and/or GPS).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequestDTO {

    @NotNull(message = "Check-in method is required (QR or MANUAL)")
    private String method;  // "QR" or "MANUAL"

    @NotNull(message = "WiFi SSID is required for geofencing verification")
    private String wifiSSID;  // Detected WiFi network name (e.g., "SLT-Fiber-5G_6df8")

    private String wifiBSSID;  // WiFi MAC address (optional, for stricter verification)

    @NotNull(message = "Latitude is required for GPS geofencing verification")
    private Double latitude;  // User's current GPS latitude

    @NotNull(message = "Longitude is required for GPS geofencing verification")
    private Double longitude;  // User's current GPS longitude
}
