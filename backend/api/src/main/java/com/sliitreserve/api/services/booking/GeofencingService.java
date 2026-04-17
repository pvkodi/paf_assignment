package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * GeofencingService for WiFi and GPS-based location verification.
 * 
 * Supports two geofencing methods:
 * 1. WiFi-based: Verify user device is connected to facility WiFi (SSID + optional MAC)
 * 2. GPS-based: Verify user is within facility GPS radius (Haversine distance calculation)
 * 
 * Used by CheckInService to enforce location-based check-in restrictions (FR-020 enhancement).
 * 
 * Design pattern: Service layer (geospatial business logic)
 * Dependencies: FacilityRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeofencingService {

    private final FacilityRepository facilityRepository;

    /**
     * Verify user device is connected to facility WiFi.
     * 
     * Check-in via QR is only allowed if user is on the facility's WiFi network.
     * This prevents users from checking in remotely or from outside the facility.
     * 
     * Verification method:
     * - REQUIRED: Detected SSID must match facility SSID (case-insensitive)
     * - OPTIONAL: Detected BSSID (MAC) can be validated for stricter security
     * 
     * @param facilityId Facility ID
     * @param detectedSSID WiFi SSID detected from user's device (e.g., "SLT-Fiber-5G_6df8")
     * @param detectedBSSID WiFi MAC address from user's device (optional, for stricter verification)
     * @return true if device is on facility WiFi, false otherwise
     * @throws ResourceNotFoundException if facility not found
     */
    @Transactional(readOnly = true)
    public boolean isUserOnFacilityWiFi(UUID facilityId, String detectedSSID, String detectedBSSID) {
        log.debug("--- isUserOnFacilityWiFi() called: facilityId={}, detectedSSID={}, detectedBSSID={}", 
                  facilityId, detectedSSID, detectedBSSID);
        
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + facilityId));
        
        log.debug("Facility found: {} - WiFi SSID configured: {}", facility.getName(), facility.getWifiSSID());
        
        // If facility has no WiFi configured, skip WiFi check
        if (facility.getWifiSSID() == null || facility.getWifiSSID().isBlank()) {
            log.info("⚠ Facility {} has no WiFi configured - skipping WiFi check", facilityId);
            return true;  // Skip check if not configured
        }
        
        // Detected SSID is required
        if (detectedSSID == null || detectedSSID.isBlank()) {
            log.error("❌ No WiFi SSID detected from user device - check-in BLOCKED");
            return false;
        }
        
        // Check SSID match (case-insensitive)
        boolean ssidMatches = facility.getWifiSSID().equalsIgnoreCase(detectedSSID);
        
        log.debug("WiFi SSID comparison: required='{}' vs detected='{}' (case-insensitive)",
                  facility.getWifiSSID(), detectedSSID);
        
        if (!ssidMatches) {
            log.error("❌ WiFi SSID MISMATCH at facility '{}': required='{}', detected='{}'",
                     facility.getName(), facility.getWifiSSID(), detectedSSID);
            return false;
        }
        
        // Optional: Check MAC address (BSSID) for stricter verification
        if (facility.getWifiMacAddress() != null && !facility.getWifiMacAddress().isBlank()) {
            if (detectedBSSID == null || detectedBSSID.isBlank()) {
                log.warn("Facility {} requires MAC verification but none provided", facilityId);
                // Only require MAC if facility explicitly configured it
                // For now, allow SSID match without MAC verification
            } else {
                boolean macMatches = facility.getWifiMacAddress().equalsIgnoreCase(detectedBSSID);
                log.debug("WiFi MAC comparison: required='{}' vs detected='{}' (case-insensitive)",
                          facility.getWifiMacAddress(), detectedBSSID);
                if (!macMatches) {
                    log.warn("WiFi MAC mismatch at facility {}: expected='{}', detected='{}' - ignoring (SSID match sufficient)",
                             facilityId, facility.getWifiMacAddress(), detectedBSSID);
                    // MAC mismatch is less critical; SSID match is sufficient
                    // Return true since SSID matched (MAC is optional)
                }
            }
        }
        
        log.info("✓ WiFi verification PASSED: SSID='{}' matched for facility '{}'", 
                 detectedSSID, facility.getName());
        return true;
    }

    /**
     * Verify user is within GPS radius of facility.
     * 
     * Check-in can be verified using GPS coordinates as a fallback if WiFi is unavailable
     * or as a secondary verification layer.
     * 
     * Calculation: Uses Haversine formula to compute great-circle distance between
     * facility GPS coordinates and user's current GPS coordinates.
     * 
     * @param facilityId Facility ID
     * @param userLatitude User's GPS latitude (decimal degrees)
     * @param userLongitude User's GPS longitude (decimal degrees)
     * @return true if user is within facility geofence radius, false otherwise
     * @throws ResourceNotFoundException if facility not found
     */
    @Transactional(readOnly = true)
    public boolean isUserWithinGPSRadius(UUID facilityId, Double userLatitude, Double userLongitude) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + facilityId));
        
        // If facility has no GPS configured, skip GPS check
        if (facility.getLatitude() == null || facility.getLongitude() == null) {
            log.debug("Facility {} has no GPS coordinates configured for geofencing", facilityId);
            return true;  // Skip check if not configured
        }
        
        // User GPS coordinates are required
        if (userLatitude == null || userLongitude == null) {
            log.warn("No GPS coordinates detected from user device at facility {}", facilityId);
            return false;
        }
        
        // Calculate distance
        double distanceMeters = calculateHaversineDistance(
            facility.getLatitude(),
            facility.getLongitude(),
            userLatitude,
            userLongitude
        );
        
        int radiusMeters = facility.getGeofenceRadiusMeters() != null ? 
                          facility.getGeofenceRadiusMeters() : 100;
        
        boolean withinRadius = distanceMeters <= radiusMeters;
        
        log.info("GPS verification at facility {}: distance={}m, radius={}m, within={}",
                 facilityId, String.format("%.1f", distanceMeters), radiusMeters, withinRadius);
        
        if (!withinRadius) {
            log.warn("User is {}.1fm outside facility geofence (radius: {}m)",
                     String.format("%.1f", distanceMeters - radiusMeters), radiusMeters);
        }
        
        return withinRadius;
    }

    /**
     * Calculate distance between two GPS coordinates using Haversine formula.
     * 
     * Formula: d = 2R * arcsin(sqrt(sin²(Δlat/2) + cos(lat1)*cos(lat2)*sin²(Δlon/2)))
     * 
     * Where:
     * - R = Earth radius (6371 km)
     * - lat/lon in radians
     * 
     * @param lat1 First latitude (decimal degrees)
     * @param lon1 First longitude (decimal degrees)
     * @param lat2 Second latitude (decimal degrees)
     * @param lon2 Second longitude (decimal degrees)
     * @return Distance in meters
     */
    protected double calculateHaversineDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;
        
        // Convert degrees to radians
        double latRadians1 = Math.toRadians(lat1);
        double latRadians2 = Math.toRadians(lat2);
        double deltaLatRadians = Math.toRadians(lat2 - lat1);
        double deltaLonRadians = Math.toRadians(lon2 - lon1);
        
        // Haversine formula
        double a = Math.sin(deltaLatRadians / 2) * Math.sin(deltaLatRadians / 2) +
                   Math.cos(latRadians1) * Math.cos(latRadians2) *
                   Math.sin(deltaLonRadians / 2) * Math.sin(deltaLonRadians / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;
        
        return distanceKm * 1000;  // Convert to meters
    }

    /**
     * Get facility geofencing status (info for logging/debugging).
     * 
     * @param facilityId Facility ID
     * @return Formatted string with geofencing configuration
     */
    @Transactional(readOnly = true)
    public String getGeofencingStatus(UUID facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + facilityId));
        
        StringBuilder status = new StringBuilder();
        status.append("Geofencing Status for ").append(facility.getName()).append(":\n");
        
        if (facility.getWifiSSID() != null) {
            status.append("  ✓ WiFi: ").append(facility.getWifiSSID());
            if (facility.getWifiMacAddress() != null) {
                status.append(" (MAC: ").append(facility.getWifiMacAddress()).append(")");
            }
            status.append("\n");
        } else {
            status.append("  ✗ WiFi: Not configured\n");
        }
        
        if (facility.getLatitude() != null && facility.getLongitude() != null) {
            status.append("  ✓ GPS: ").append(facility.getLatitude()).append(", ")
                   .append(facility.getLongitude()).append(" (Radius: ")
                   .append(facility.getGeofenceRadiusMeters() != null ? facility.getGeofenceRadiusMeters() : 100)
                   .append("m)\n");
        } else {
            status.append("  ✗ GPS: Not configured\n");
        }
        
        return status.toString();
    }
}
