package com.sliitreserve.api.contract.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Tests for Approval, Check-In, and Appeal Endpoints (OpenAPI Compliance)
 * 
 * Purpose: Verify that approval workflow, check-in, and suspension appeal endpoints
 * conform to the contract defined in specs/001-feat-pamali-smart-campus-ops-hub/contracts/openapi.yaml
 * 
 * Test Scope:
 * 1. POST /bookings/{bookingId}/approve - Approve booking step
 * 2. POST /bookings/{bookingId}/check-in - Record check-in with QR/manual method
 * 3. POST /appeals - Submit suspension appeal with justification
 * 4. Response schema validation (required fields, types, enums)
 * 5. HTTP status codes per contract (200, 201, 400, 403, 404)
 * 6. Error response structure compliance
 * 7. RBAC enforcement (who can approve, check-in, appeal)
 * 8. Business rule validation (e.g., check-in outside grace period)
 * 
 * @see com.sliitreserve.api.controllers.BookingController
 * @see com.sliitreserve.api.controllers.AppealController
 * @see com.sliitreserve.api.workflow.approval.ApprovalHandler
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Approval, Check-In, and Appeal Contract Tests")
class ApprovalAndAppealContractTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BOOKINGS_ENDPOINT = "/api/v1/bookings";
    private static final String APPEALS_ENDPOINT = "/api/v1/appeals";
    private static final String APPROVE_SUFFIX = "/approve";
    private static final String CHECK_IN_SUFFIX = "/check-in";

    private UUID testBookingId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testBookingId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("POST /bookings/{bookingId}/approve - Booking Approval Contract Tests")
    class BookingApprovalContractTests {

        @Test
        @DisplayName("Should return 200 with updated booking when approval succeeds")
        void testApproveBookingSuccess() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(testBookingId.toString())))
                    .andExpect(jsonPath("$.status", anyOf(
                            is("APPROVED"),
                            is("PENDING") // May still be PENDING if more approvals needed
                    )))
                    .andExpect(jsonPath("$.requestedBy", notNullValue()))
                    .andExpect(jsonPath("$.facility", notNullValue()));
        }

        @Test
        @DisplayName("Should return 404 when booking not found")
        void testApproveBookingNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + nonExistentId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("not found")));
        }

        @Test
        @DisplayName("Should return 403 when user lacks approval authority")
        void testApproveBookingForbidden() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer user-without-approve-role"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("not authorized")));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void testApproveBookingUnauthorized() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()));
        }

        @Test
        @DisplayName("Should include approval step details in response")
        void testApproveBookingResponseStructure() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", allOf(
                            hasKey("id"),
                            hasKey("status"),
                            hasKey("facility"),
                            hasKey("requestedBy"),
                            hasKey("bookingDate"),
                            hasKey("startTime"),
                            hasKey("endTime"),
                            hasKey("attendees"),
                            hasKey("createdAt")
                    )));
        }
    }

    @Nested
    @DisplayName("POST /bookings/{bookingId}/check-in - Check-In Contract Tests")
    class CheckInContractTests {

        @Test
        @DisplayName("Should return 200 when check-in succeeds")
        void testCheckInSuccess() throws Exception {
            String checkInRequest = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "method", "QR_SCAN",
                            "qrCode", "booking-qr-code-123"
                    )
            );

            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + CHECK_IN_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return 400 when check-in request is invalid")
        void testCheckInBadRequest() throws Exception {
            String invalidRequest = objectMapper.writeValueAsString(
                    java.util.Map.of("method", "INVALID_METHOD")
            );

            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + CHECK_IN_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("invalid")));
        }

        @Test
        @DisplayName("Should return 404 when booking not found")
        void testCheckInBookingNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            String checkInRequest = objectMapper.writeValueAsString(
                    java.util.Map.of("method", "MANUAL")
            );

            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + nonExistentId + CHECK_IN_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()));
        }

        @Test
        @DisplayName("Should return 403 when user not authorized to check in")
        void testCheckInForbidden() throws Exception {
            String checkInRequest = objectMapper.writeValueAsString(
                    java.util.Map.of("method", "MANUAL")
            );

            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + CHECK_IN_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInRequest)
                    .header("Authorization", "Bearer different-user-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("not authorized")));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void testCheckInUnauthorized() throws Exception {
            String checkInRequest = objectMapper.writeValueAsString(
                    java.util.Map.of("method", "MANUAL")
            );

            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + CHECK_IN_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkInRequest))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should accept both QR_SCAN and MANUAL methods")
        void testCheckInMethodVariations() throws Exception {
            String[] methods = {"QR_SCAN", "MANUAL"};

            for (String method : methods) {
                String checkInRequest = objectMapper.writeValueAsString(
                        java.util.Map.of("method", method)
                );

                mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + CHECK_IN_SUFFIX)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInRequest)
                        .header("Authorization", "Bearer valid-jwt-token"))
                        .andExpect(status().isOk());
            }
        }

        @Test
        @DisplayName("Should validate request body structure")
        void testCheckInRequestValidation() throws Exception {
            // Missing required 'method' field
            String invalidRequest = objectMapper.writeValueAsString(
                    java.util.Map.of("qrCode", "some-code")
            );

            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + CHECK_IN_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /appeals - Suspension Appeal Contract Tests")
    class SuspensionAppealContractTests {

        @Test
        @DisplayName("Should return 201 when appeal is submitted successfully")
        void testSubmitAppealSuccess() throws Exception {
            String appealRequest = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "justification", "I was unable to attend due to illness. Attached medical certificate.",
                            "attachment_urls", java.util.List.of("https://example.com/cert.pdf")
                    )
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(appealRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.submittedAt", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 when appeal request is invalid")
        void testSubmitAppealBadRequest() throws Exception {
            // Missing required justification field
            String invalidRequest = objectMapper.writeValueAsString(
                    java.util.Map.of("attachment_urls", java.util.List.of())
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("required")));
        }

        @Test
        @DisplayName("Should return 403 when user not suspended or not authorized")
        void testSubmitAppealForbidden() throws Exception {
            String appealRequest = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "justification", "I did not violate the policy"
                    )
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(appealRequest)
                    .header("Authorization", "Bearer token-non-suspended-user"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error", notNullValue()))
                    .andExpect(jsonPath("$.message", containsString("suspended")));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void testSubmitAppealUnauthorized() throws Exception {
            String appealRequest = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "justification", "I have a valid reason for my no-shows"
                    )
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(appealRequest))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should validate justification field length")
        void testSubmitAppealJustificationValidation() throws Exception {
            // Justification too short (assume minimum 10 chars)
            String shortJustification = objectMapper.writeValueAsString(
                    java.util.Map.of("justification", "short")
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(shortJustification)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should accept optional attachment URLs")
        void testSubmitAppealWithAttachments() throws Exception {
            String appealRequest = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "justification", "Legitimate reason with medical evidence",
                            "attachment_urls", java.util.List.of(
                                    "https://example.com/medical-cert.pdf",
                                    "https://example.com/prescription.pdf"
                            )
                    )
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(appealRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()));
        }

        @Test
        @DisplayName("Should include appeal response structure with all required fields")
        void testSubmitAppealResponseStructure() throws Exception {
            String appealRequest = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "justification", "I was ill and have medical documentation"
                    )
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(appealRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", allOf(
                            hasKey("id"),
                            hasKey("status"),
                            hasKey("submittedAt"),
                            hasKey("submittedBy"),
                            hasKey("justification")
                    )));
        }
    }

    @Nested
    @DisplayName("Error Response Contract Tests")
    class ErrorResponseContractTests {

        @Test
        @DisplayName("Should return error response with consistent structure for 400")
        void testErrorResponseStructure400() throws Exception {
            String invalidRequest = objectMapper.writeValueAsString(
                    java.util.Map.of()
            );

            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", allOf(
                            hasKey("error"),
                            hasKey("message")
                    )));
        }

        @Test
        @DisplayName("Should return error response with consistent structure for 403")
        void testErrorResponseStructure403() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer user-without-role"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$", allOf(
                            hasKey("error"),
                            hasKey("message")
                    )));
        }

        @Test
        @DisplayName("Should return error response with consistent structure for 404")
        void testErrorResponseStructure404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + nonExistentId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$", allOf(
                            hasKey("error"),
                            hasKey("message")
                    )));
        }
    }

    @Nested
    @DisplayName("Content Type and Encoding Contract Tests")
    class ContentTypeContractTests {

        @Test
        @DisplayName("Should accept application/json for approval endpoint")
        void testApprovalAcceptsJsonContentType() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject requests with unsupported content types")
        void testRejectUnsupportedContentType() throws Exception {
            mockMvc.perform(post(APPEALS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_XML)
                    .content("<appeal></appeal>")
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should return application/json content type in responses")
        void testResponseContentTypeIsJson() throws Exception {
            mockMvc.perform(post(BOOKINGS_ENDPOINT + "/" + testBookingId + APPROVE_SUFFIX)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
