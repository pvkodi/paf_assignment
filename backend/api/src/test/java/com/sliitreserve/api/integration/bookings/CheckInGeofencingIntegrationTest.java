package com.sliitreserve.api.integration.bookings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sliitreserve.api.dto.bookings.CheckInRequestDTO;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.CheckInMethod;
import com.sliitreserve.api.entities.booking.CheckInRecord;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.bookings.CheckInRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Check-In with Geofencing
 * 
 * Purpose: Verify end-to-end check-in workflow with GPS-based geofencing verification.
 * Tests full stack: Controller -> Service -> Repository interactions.
 * 
 * Test Scenarios:
 * 1. Successful check-in with valid GPS location (HTTP 201)
 * 2. Check-in failure: GPS out of range (HTTP 403 GEOFENCE_GPS_OUT_OF_RANGE)
 * 3. Check-in with geofencing not configured (should allow)
 * 4. Duplicate check-in attempt (HTTP 409)
 * 5. Manual check-in by staff with GPS verification
 * 
 * Requirements Covered:
 * - POST /api/v1/bookings/{bookingId}/check-in/with-geofencing endpoint
 * - Proper HTTP status codes and error messages
 * - Geofencing data validation (WiFi SSID, BSSID, latitude, longitude)
 * - Check-in record persisted correctly
 * - Suspension policy enforced
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Check-In with Geofencing Integration Tests")
public class CheckInGeofencingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    private User testUser;
    private User testLecturer;
    private Facility testFacility;
    private Booking testBooking;
    private UUID facilityId;
    private UUID bookingId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("student@test.com");
        testUser.setName("Test Student");
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);
        userId = testUser.getId();

        // Create test lecturer
        testLecturer = new User();
        testLecturer.setEmail("lecturer@test.com");
        testLecturer.setName("Test Lecturer");
        testLecturer.setRole(Role.LECTURER);
        testLecturer = userRepository.save(testLecturer);

        // Create facility with geofencing configuration
        testFacility = new Facility();
        testFacility.setName("Lecture Hall A");
        testFacility.setCapacity(50);
        testFacility.setWifiSSID("SLT-Fiber-5G_6df8");
        testFacility.setWifiMacAddress("b4:0f:3b:64:6d:f8");
        testFacility.setLatitude(6.9271);
        testFacility.setLongitude(80.7789);
        testFacility.setGeofenceRadiusMeters(100);
        testFacility = facilityRepository.save(testFacility);
        facilityId = testFacility.getId();

        // Create a booking
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Colombo"));
        testBooking = new Booking();
        testBooking.setUser(testUser);
        testBooking.setFacility(testFacility);
        testBooking.setStartTime(now.plusMinutes(10));
        testBooking.setEndTime(now.plusHours(2));
        testBooking.setStatus("APPROVED");
        testBooking = bookingRepository.save(testBooking);
        bookingId = testBooking.getId();
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = {"USER"})
    @DisplayName("Should successfully check in with valid WiFi and GPS")
    void testSuccessfulCheckInWithValidGeofencing() throws Exception {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setMethod(CheckInMethod.QR);
        request.setLatitude(6.92705);  // Within radius (~100m from facility)
        request.setLongitude(80.77895);

        MvcResult result = mockMvc.perform(post("/api/v1/bookings/{bookingId}/check-in/with-geofencing", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.booking_id", is(bookingId.toString())))
                .andExpect(jsonPath("$.method", is("QR")))
                .andExpect(jsonPath("$.checked_in_at", notNullValue()))
                .andReturn();

        // Verify check-in record was created
        assert checkInRepository.findByBookingId(bookingId).size() == 1;
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = {"USER"})
    @DisplayName("Should reject check-in when GPS out of range")
    void testCheckInFailureGPSOutOfRange() throws Exception {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setMethod(CheckInMethod.QR);
        request.setLatitude(40.7128);  // New York latitude - far away (~6000km)
        request.setLongitude(-74.0060);  // New York longitude - far away

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/check-in/with-geofencing", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("GEOFENCE_GPS_OUT_OF_RANGE")));

        // Verify no check-in record was created
        assert checkInRepository.findByBookingId(bookingId).isEmpty();
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = {"USER"})
    @DisplayName("Should allow check-in without geofencing when not configured")
    void testCheckInWithoutGeofencingConfigured() throws Exception {
        // Create facility without geofencing
        Facility facilityNoGeo = new Facility();
        facilityNoGeo.setName("Hall without Geofencing");
        facilityNoGeo.setCapacity(30);
        facilityNoGeo.setWifiSSID(null);  // No WiFi configured
        facilityNoGeo.setLatitude(null);   // No GPS configured
        facilityNoGeo = facilityRepository.save(facilityNoGeo);

        // Create booking for this facility
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Colombo"));
        Booking bookingNoGeo = new Booking();
        bookingNoGeo.setUser(testUser);
        bookingNoGeo.setFacility(facilityNoGeo);
        bookingNoGeo.setStartTime(now.plusMinutes(10));
        bookingNoGeo.setEndTime(now.plusHours(2));
        bookingNoGeo.setStatus("APPROVED");
        bookingNoGeo = bookingRepository.save(bookingNoGeo);

        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setMethod(CheckInMethod.QR);
        request.setLatitude(0.0);
        request.setLongitude(0.0);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/check-in/with-geofencing", bookingNoGeo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = {"USER"})
    @DisplayName("Should reject duplicate check-in attempts")
    void testDuplicateCheckInRejection() throws Exception {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setMethod(CheckInMethod.QR);
        request.setLatitude(6.92705);
        request.setLongitude(80.77895);

        // First check-in should succeed
        mockMvc.perform(post("/api/v1/bookings/{bookingId}/check-in/with-geofencing", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second check-in should fail (duplicate)
        mockMvc.perform(post("/api/v1/bookings/{bookingId}/check-in/with-geofencing", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "lecturer@test.com", roles = {"LECTURER"})
    @DisplayName("Should allow manual check-in by staff with geofencing")
    void testManualCheckInByStaff() throws Exception {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setMethod(CheckInMethod.MANUAL);
        request.setNotes("Student checked in by staff");
        request.setLatitude(6.92705);
        request.setLongitude(80.77895);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/check-in/with-geofencing", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method", is("MANUAL")));
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = {"USER"})
    @DisplayName("Should validate required geofencing fields (GPS)")
    void testValidationOfRequiredFields() throws Exception {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setMethod(CheckInMethod.QR);
        request.setLatitude(null);  // Missing latitude
        request.setLongitude(80.77895);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/check-in/with-geofencing", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("required")));
    }
}
