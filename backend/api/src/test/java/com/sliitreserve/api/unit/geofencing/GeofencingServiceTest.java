package com.sliitreserve.api.unit.geofencing;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.services.booking.GeofencingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for GeofencingService
 * 
 * Purpose: Verify GPS-based geofencing logic for location verification during check-in.
 * Note: WiFi detection is not available in standard web browsers, so tests focus on GPS.
 * 
 * Test Scope:
 * - GPS distance calculation (Haversine formula)
 * - GPS radius verification
 * - Facility without geofencing configured
 * - Edge cases (null values, missing configurations)
 * 
 * Requirements covered:
 * - GPS must use Haversine formula for accurate distance calculation
 * - Facilities without geofencing bypass verification
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeofencingService Unit Tests")
public class GeofencingServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @InjectMocks
    private GeofencingService geofencingService;

    private Facility testFacility;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        facilityId = UUID.randomUUID();
        testFacility = new Facility();
        testFacility.setId(facilityId);
        testFacility.setName("Lecture Hall A");
        testFacility.setWifiSSID("SLT-Fiber-5G_6df8");
        testFacility.setWifiMacAddress("b4:0f:3b:64:6d:f8");
        testFacility.setLatitude(6.9271);
        testFacility.setLongitude(80.7789);
        testFacility.setGeofenceRadiusMeters(100);
    }

    // WiFi SSID Verification Tests - REMOVED (WiFi detection not available in browsers)
    // Geofencing now uses GPS-only verification

    @Nested
    @DisplayName("GPS Distance Verification Tests")
    class GPSDistanceVerificationTests {

        @Test
        @DisplayName("Should verify user within GPS radius")
        void testUserWithinRadius() {
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            // Campus location: 6.9271, 80.7789
            // User location: ~50m away (approximately 6.92705, 80.77895)
            boolean result = geofencingService.isUserWithinGPSRadius(
                facilityId,
                6.92705,  // Slightly offset latitude
                80.77895  // Slightly offset longitude
            );

            assertTrue(result);
        }

        @Test
        @DisplayName("Should fail if user outside GPS radius")
        void testUserOutsideRadius() {
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            // Campus location: 6.9271, 80.7789
            // User location: ~500m away
            boolean result = geofencingService.isUserWithinGPSRadius(
                facilityId,
                6.9326,   // ~5km difference
                80.7889
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return true if facility GPS not configured")
        void testNoGPSConfigured() {
            testFacility.setLatitude(null);
            testFacility.setLongitude(null);
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            boolean result = geofencingService.isUserWithinGPSRadius(
                facilityId,
                6.9271,
                80.7789
            );

            assertTrue(result);  // Skip check if not configured
        }

        @Test
        @DisplayName("Should return false if user GPS not provided")
        void testUserGPSNotProvided() {
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            boolean result = geofencingService.isUserWithinGPSRadius(
                facilityId,
                null,  // No GPS coordinates
                null
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("Should use custom geofence radius")
        void testCustomGeofenceRadius() {
            testFacility.setGeofenceRadiusMeters(50);  // 50m radius
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            // User 60m away should fail with 50m radius
            boolean result = geofencingService.isUserWithinGPSRadius(
                facilityId,
                6.9276,  // ~60m away
                80.7789
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("Should use default radius when not configured")
        void testDefaultGeofenceRadius() {
            testFacility.setGeofenceRadiusMeters(null);
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            // Within default 100m
            boolean result = geofencingService.isUserWithinGPSRadius(
                facilityId,
                6.92705,
                80.77895
            );

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Haversine Distance Calculation Tests")
    class HaversineDistanceCalculationTests {

        @Test
        @DisplayName("Should calculate distance between two points")
        void testDistanceCalculation() {
            // Colombo: 6.9271, 80.7789
            // Calculated distance to nearby point should be ~50-100m
            double distance = geofencingService.calculateHaversineDistance(
                6.9271,
                80.7789,
                6.92705,  // Slightly offset
                80.77895
            );

            assertTrue(distance >= 0, "Distance should be non-negative");
            assertTrue(distance < 200, "Distance should be less than 200m for small offsets");
        }

        @Test
        @DisplayName("Should return zero distance for same coordinates")
        void testZeroDistance() {
            double distance = geofencingService.calculateHaversineDistance(
                6.9271,
                80.7789,
                6.9271,
                80.7789
            );

            assertEquals(0, distance, 0.1, "Distance for same coordinates should be ~0");
        }

        @Test
        @DisplayName("Should calculate distance for distant cities")
        void testLargeDistance() {
            // Colombo: 6.9271, 80.7789
            // New York: 40.7128, -74.0060
            // Approximate distance: ~13000 km
            double distance = geofencingService.calculateHaversineDistance(
                6.9271,
                80.7789,
                40.7128,
                -74.0060
            );

            assertTrue(distance > 10_000_000, "Distance should be > 10,000 km in meters");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle both WiFi and GPS verification")
        void testBothVerifications() {
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            // WiFi match
            boolean wifiResult = geofencingService.isUserOnFacilityWiFi(
                facilityId,
                "SLT-Fiber-5G_6df8",
                "b4:0f:3b:64:6d:f8"
            );

            // GPS within radius
            boolean gpsResult = geofencingService.isUserWithinGPSRadius(
                facilityId,
                6.92705,
                80.77895
            );

            assertTrue(wifiResult);
            assertTrue(gpsResult);
        }

        @Test
        @DisplayName("Should fail if either WiFi or GPS fails")
        void testEitherVerificationFails() {
            when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(testFacility));

            // WiFi match
            boolean wifiResult = geofencingService.isUserOnFacilityWiFi(
                facilityId,
                "SLT-Fiber-5G_6df8",
                "b4:0f:3b:64:6d:f8"
            );

            // GPS outside radius (from different country)
            boolean gpsResult = geofencingService.isUserWithinGPSRadius(
                facilityId,
                40.7128,   // New York latitude
                -74.0060   // New York longitude
            );

            assertTrue(wifiResult);
            assertFalse(gpsResult);
        }
    }
}
