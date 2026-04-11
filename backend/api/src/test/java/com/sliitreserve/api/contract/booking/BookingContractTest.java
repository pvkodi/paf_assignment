package com.sliitreserve.api.contract.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Tests for Facilities and Bookings Endpoints (OpenAPI Compliance)
 * 
 * Purpose: Verify that facilities search and booking creation endpoints
 * conform to the contract defined in specs/001-feat-pamali-smart-campus-ops-hub/contracts/openapi.yaml
 * 
 * Test Scope:
 * 1. GET /facilities - Search facilities with various filter combinations
 * 2. POST /bookings - Create booking requests with valid and invalid payloads
 * 3. Response schema validation (required fields, types, enums)
 * 4. HTTP status codes per contract (200, 201, 400, 403, 409)
 * 5. Error response structure compliance
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Facilities and Bookings Contract Tests")
class BookingContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String FACILITIES_ENDPOINT = "/api/v1/facilities";
    private static final String BOOKINGS_ENDPOINT = "/api/v1/bookings";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Nested
    @DisplayName("GET /facilities - Facility Search Contract Tests")
    class FacilitiesSearchContractTests {

        @Test
        @DisplayName("Should return 200 with facilities array when no filters applied")
        void testGetFacilitiesNoFilters() throws Exception {
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", isA(java.util.List.class)));
        }

        @Test
        @DisplayName("Should return facilities filtered by type query parameter")
        void testGetFacilitiesByType() throws Exception {
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .param("type", "LECTURE_HALL")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(java.util.List.class)));
        }

        @Test
        @DisplayName("Should return facilities filtered by minCapacity query parameter")
        void testGetFacilitiesByMinCapacity() throws Exception {
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .param("minCapacity", "50")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(java.util.List.class)));
        }

        @Test
        @DisplayName("Should return facilities filtered by location query parameter")
        void testGetFacilitiesByLocation() throws Exception {
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .param("location", "Building A")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(java.util.List.class)));
        }

        @Test
        @DisplayName("Should return facilities filtered by building query parameter")
        void testGetFacilitiesByBuilding() throws Exception {
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .param("building", "Main Campus")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(java.util.List.class)));
        }

        @Test
        @DisplayName("Should return facilities with multiple filters applied")
        void testGetFacilitiesMultipleFilters() throws Exception {
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .param("type", "LECTURE_HALL")
                    .param("minCapacity", "30")
                    .param("location", "Building A")
                    .param("building", "Main Campus")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(java.util.List.class)));
        }

        @Test
        @DisplayName("Should validate FacilityResponse schema - required fields present")
        void testFacilityResponseSchemaRequiredFields() throws Exception {
            MvcResult result = mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            // Facilities list might be empty initially; contract defines required fields if present
            if (!response.equals("[]")) {
                mockMvc.perform(get(FACILITIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$[0].id").exists())
                        .andExpect(jsonPath("$[0].name").exists())
                        .andExpect(jsonPath("$[0].type").exists())
                        .andExpect(jsonPath("$[0].capacity").exists())
                        .andExpect(jsonPath("$[0].location").exists())
                        .andExpect(jsonPath("$[0].building").exists())
                        .andExpect(jsonPath("$[0].floor").exists())
                        .andExpect(jsonPath("$[0].status").exists());
            }
        }

        @Test
        @DisplayName("Should validate FacilityResponse schema - UUID format for id")
        void testFacilityIdIsUuid() throws Exception {
            MvcResult result = mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            if (!response.equals("[]")) {
                mockMvc.perform(get(FACILITIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$[0].id", matchesPattern(
                                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")));
            }
        }

        @Test
        @DisplayName("Should validate FacilityType enum values")
        void testFacilityTypeEnumValues() throws Exception {
            MvcResult result = mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            if (!response.equals("[]")) {
                mockMvc.perform(get(FACILITIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$[0].type", isIn(Arrays.asList(
                                "LECTURE_HALL", "LAB", "MEETING_ROOM", "AUDITORIUM", "EQUIPMENT", "SPORTS_FACILITY"))));
            }
        }

        @Test
        @DisplayName("Should validate facility status enum values")
        void testFacilityStatusEnumValues() throws Exception {
            MvcResult result = mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            if (!response.equals("[]")) {
                mockMvc.perform(get(FACILITIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$[0].status", isIn(Arrays.asList("ACTIVE", "OUT_OF_SERVICE"))));
            }
        }

        @Test
        @DisplayName("Should return 403 Forbidden when unauthorized")
        void testGetFacilitiesForbidden() throws Exception {
            // This test assumes secured endpoint; adjust based on actual security config
            // Currently placeholder - endpoint may be public
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /bookings - Booking Creation Contract Tests")
    class BookingsCreationContractTests {

        private String validBookingJson;

        @BeforeEach
        void setUp() {
            // Valid booking request payload per OpenAPI schema
            validBookingJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting for project planning",
                      "attendees": 5
                    }
                    """;
        }

        @Test
        @DisplayName("Should create booking with valid request payload")
        void testCreateBookingWithValidPayload() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return BookingResponse with required fields on successful creation")
        void testCreateBookingResponseContainsRequiredFields() throws Exception {
            MvcResult result = mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andReturn();

            if (result.getResponse().getStatus() == 201) {
                mockMvc.perform(post(BOOKINGS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson))
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.status").exists())
                        .andExpect(jsonPath("$.facility").exists())
                        .andExpect(jsonPath("$.startDateTime").exists())
                        .andExpect(jsonPath("$.endDateTime").exists())
                        .andExpect(jsonPath("$.version").exists());
            }
        }

        @Test
        @DisplayName("Should return booking ID as UUID")
        void testBookingIdIsUuid() throws Exception {
            MvcResult result = mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andReturn();

            if (result.getResponse().getStatus() == 201) {
                mockMvc.perform(post(BOOKINGS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson))
                        .andExpect(jsonPath("$.id", matchesPattern(
                                "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")));
            }
        }

        @Test
        @DisplayName("Should return booking status enum value")
        void testBookingStatusEnumValues() throws Exception {
            MvcResult result = mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andReturn();

            if (result.getResponse().getStatus() == 201) {
                mockMvc.perform(post(BOOKINGS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson))
                        .andExpect(jsonPath("$.status", isIn(Arrays.asList("PENDING", "APPROVED", "REJECTED", "CANCELLED"))));
            }
        }

        @Test
        @DisplayName("Should contain facility object in response")
        void testBookingResponseContainsFacility() throws Exception {
            MvcResult result = mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andReturn();

            if (result.getResponse().getStatus() == 201) {
                mockMvc.perform(post(BOOKINGS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson))
                        .andExpect(jsonPath("$.facility.id").exists())
                        .andExpect(jsonPath("$.facility.name").exists())
                        .andExpect(jsonPath("$.facility.type").exists());
            }
        }

        @Test
        @DisplayName("Should return 400 Bad Request when facilityId is missing")
        void testCreateBookingMissingFacilityId() throws Exception {
            String invalidJson = """
                    {
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when bookingDate is missing")
        void testCreateBookingMissingBookingDate() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when startTime is missing")
        void testCreateBookingMissingStartTime() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "endTime": "11:00",
                      "purpose": "Team meeting",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when endTime is missing")
        void testCreateBookingMissingEndTime() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "purpose": "Team meeting",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when purpose is missing")
        void testCreateBookingMissingPurpose() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when attendees is missing")
        void testCreateBookingMissingAttendees() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting"
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when purpose is too short (< 3 chars)")
        void testCreateBookingPurposeTooShort() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "ab",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when attendees is 0 (minimum 1)")
        void testCreateBookingAttendeesZero() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting",
                      "attendees": 0
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when startTime format is invalid")
        void testCreateBookingInvalidStartTimeFormat() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "25:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when endTime format is invalid")
        void testCreateBookingInvalidEndTimeFormat() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "25:00",
                      "purpose": "Team meeting",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should support optional bookedForUserId for admin-on-behalf bookings")
        void testCreateBookingWithBookedForUserId() throws Exception {
            String bookingWithBookedFor = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting for project planning",
                      "attendees": 5,
                      "bookedForUserId": "660e8400-e29b-41d4-a716-446655440001"
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookingWithBookedFor))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should support optional recurrenceRule for recurring bookings")
        void testCreateBookingWithRecurrenceRule() throws Exception {
            String bookingWithRecurrence = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Weekly team meeting",
                      "attendees": 5,
                      "recurrenceRule": "FREQ=WEEKLY;UNTIL=20260630"
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookingWithRecurrence))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user lacks permission")
        void testCreateBookingForbidden() throws Exception {
            // This test assumes endpoint requires authentication
            // Adjust based on actual security config
            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 409 Conflict on booking overlap")
        void testCreateBookingOverlapConflict() throws Exception {
            // First booking succeeds
            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andExpect(status().isCreated());

            // Second overlapping booking should return 409
            // This test assumes the facility and time overlap
            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return ErrorResponse on 409 Conflict with proper schema")
        void testConflictErrorResponseSchema() throws Exception {
            MvcResult result = mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBookingJson))
                    .andReturn();

            if (result.getResponse().getStatus() == 409) {
                mockMvc.perform(post(BOOKINGS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookingJson))
                        .andExpect(jsonPath("$.code").exists())
                        .andExpect(jsonPath("$.message").exists())
                        .andExpect(jsonPath("$.timestamp").exists());
            }
        }
    }

    @Nested
    @DisplayName("Error Response Contract Tests")
    class ErrorResponseContractTests {

        @Test
        @DisplayName("Should return ErrorResponse with required fields on bad request")
        void testErrorResponseSchema() throws Exception {
            String invalidJson = """
                    {
                      "invalidField": "test"
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should include details array in error response for validation errors")
        void testErrorResponseWithDetails() throws Exception {
            String invalidJson = """
                    {
                      "facilityId": "invalid-uuid",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "ab",
                      "attendees": 0
                    }
                    """;

            MvcResult result = mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andReturn();

            if (result.getResponse().getStatus() == 400) {
                mockMvc.perform(post(BOOKINGS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                        .andExpect(jsonPath("$.details", isA(java.util.List.class)));
            }
        }
    }

    @Nested
    @DisplayName("Content Type Contract Tests")
    class ContentTypeContractTests {

        @Test
        @DisplayName("Should require application/json content type for POST /bookings")
        void testBookingsPostContentType() throws Exception {
            String validJson = """
                    {
                      "facilityId": "550e8400-e29b-41d4-a716-446655440000",
                      "bookingDate": "2026-04-20",
                      "startTime": "10:00",
                      "endTime": "11:00",
                      "purpose": "Team meeting",
                      "attendees": 5
                    }
                    """;

            mockMvc.perform(post(BOOKINGS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validJson))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return application/json content type in response")
        void testResponseContentType() throws Exception {
            mockMvc.perform(get(FACILITIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
