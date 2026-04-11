package com.sliitreserve.api.controllers;

import com.sliitreserve.api.dto.auth.AppealDecisionRequest;
import com.sliitreserve.api.dto.auth.AppealRequest;
import com.sliitreserve.api.dto.auth.AppealResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.services.auth.AppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for suspension appeal endpoints.
 *
 * <p><b>Purpose</b>: Handles suspension appeal submission, admin review, and decision endpoints.
 * Implements FR-023 (user appeals for suspension lift with admin review).
 *
 * <p><b>Endpoints</b>:
 * <ul>
 *   <li><b>POST /api/v1/appeals</b> [PROTECTED]
 *     - Submit suspension appeal (allowed for suspended users)
 *     - Request: AppealRequest (reason)
 *     - Response: AppealResponse (201 Created)
 *   <li><b>GET /api/v1/appeals</b> [PROTECTED - ADMIN ONLY OR USER'S OWN]
 *     - List appeals (admin sees all pending; users see their own)
 *     - Response: List of AppealResponse
 *   <li><b>GET /api/v1/appeals/{id}</b> [PROTECTED]
 *     - Get appeal details
 *     - Response: AppealResponse (200 OK)
 *   <li><b>POST /api/v1/appeals/{id}/approve</b> [PROTECTED - ADMIN ONLY]
 *     - Approve appeal and release suspension
 *     - Request: AppealDecisionRequest (approved=true, decision)
 *     - Response: AppealResponse (200 OK)
 *   <li><b>POST /api/v1/appeals/{id}/reject</b> [PROTECTED - ADMIN ONLY]
 *     - Reject appeal; suspension remains
 *     - Request: AppealDecisionRequest (approved=false, decision)
 *     - Response: AppealResponse (200 OK)
 * </ul>
 *
 * <p><b>Security</b>:
 * <ul>
 *   <li>All endpoints require Bearer token (protected)
 *   <li>Suspended users allowed to submit appeals (special whitelisted operation)
 *   <li>Approve/reject endpoints admin-only (RBAC)
 *   <li>Users can only view their own appeals unless ADMIN
 * </ul>
 *
 * <p><b>Authorization Rules</b>:
 * <ul>
 *   <li>POST /appeals: Any authenticated user (but only if currently suspended)
 *   <li>GET /appeals: ADMIN (all pending) OR users see their own
 *   <li>GET /appeals/{id}: User (own appeal) OR ADMIN
 *   <li>POST /appeals/{id}/approve: ADMIN only
 *   <li>POST /appeals/{id}/reject: ADMIN only
 * </ul>
 *
 * <p><b>Error Responses</b>:
 * <ul>
 *   <li>HTTP 401: Missing or invalid Bearer token
 *   <li>HTTP 403: Insufficient permissions (non-admin attempting admin operation)
 *   <li>HTTP 404: Appeal or user not found
 *   <li>HTTP 409: User has pending appeal (duplicate submission)
 *   <li>HTTP 422: User not suspended (cannot appeal)
 *   <li>HTTP 500: Unexpected error
 * </ul>
 *
 * @see AppealService for business logic
 * @see AppealRequest for submission request
 * @see AppealResponse for response format
 */
@RestController
@RequestMapping("/api/v1/appeals")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"})
public class AppealController {

  private final AppealService appealService;
  private final UserRepository userRepository;

  /**
   * Submit a new suspension appeal.
   *
   * <p><b>Endpoint</b>: POST /api/v1/appeals
   *
   * <p><b>Authorization</b>: Protected; allowed for any authenticated user who is suspended.
   * Suspended users are explicitly whitelisted for this operation (FR-003).
   *
   * <p><b>Request</b>: AppealRequest
   * - reason: User's appeal justification (required, not blank)
   *
   * <p><b>Response</b>: AppealResponse (HTTP 201 Created)
   * - id: Appeal UUID
   * - userId: User who submitted
   * - reason: Appeal reason
   * - status: SUBMITTED
   * - createdAt: Submission timestamp
   *
   * <p><b>Error Responses</b>:
   * - HTTP 401: Invalid or missing Bearer token
   * - HTTP 404: User not found
   * - HTTP 409: User has pending appeal (prevent duplicates)
   * - HTTP 422: User not suspended
   * - HTTP 500: Unexpected error
   *
   * @param request AppealRequest with appeal reason
   * @param authentication Spring Security authentication (contains user email)
   * @return ResponseEntity with AppealResponse (201 Created)
   */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<AppealResponse> submitAppeal(
      @Valid @RequestBody AppealRequest request,
      Authentication authentication) {

    log.info("Appeal submission request received");

    String userEmail = (String) authentication.getPrincipal();
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

    AppealResponse response = appealService.submitAppeal(user.getId(), request);

    log.info("Appeal submitted successfully by user: {}", userEmail);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  /**
   * List appeals (filtered by role and identity).
   *
   * <p><b>Endpoint</b>: GET /api/v1/appeals
   *
   * <p><b>Authorization</b>: Protected
   * - ADMIN: Sees all pending appeals (review queue)
   * - USER: Sees only their own appeals
   *
   * <p><b>Query Parameters</b>: None
   *
   * <p><b>Response</b>: List of AppealResponse (HTTP 200 OK)
   *
   * <p><b>Error Responses</b>:
   * - HTTP 401: Invalid or missing Bearer token
   * - HTTP 404: User not found
   * - HTTP 500: Unexpected error
   *
   * @param authentication Spring Security authentication (contains user email)
   * @return ResponseEntity with list of AppealResponse
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<AppealResponse>> listAppeals(Authentication authentication) {

    log.debug("Appeal list request received");

    String userEmail = (String) authentication.getPrincipal();
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

    List<AppealResponse> appeals;

    // ADMIN sees all pending appeals (review queue)
    if (user.hasRole(Role.ADMIN)) {
      appeals = appealService.getPendingAppeals();
      log.debug("Admin {} retrieved {} pending appeals", userEmail, appeals.size());
    } else {
      // Regular users see only their own appeals
      appeals = appealService.getUserAppeals(user.getId());
      log.debug("User {} retrieved their {} appeals", userEmail, appeals.size());
    }

    return ResponseEntity.ok(appeals);
  }

  /**
   * Get appeal details by ID.
   *
   * <p><b>Endpoint</b>: GET /api/v1/appeals/{id}
   *
   * <p><b>Authorization</b>: Protected
   * - User can view their own appeal
   * - ADMIN can view any appeal
   *
   * <p><b>Path Parameters</b>:
   * - id: Appeal UUID
   *
   * <p><b>Response</b>: AppealResponse (HTTP 200 OK)
   *
   * <p><b>Error Responses</b>:
   * - HTTP 401: Invalid or missing Bearer token
   * - HTTP 403: Unauthorized (non-admin viewing another user's appeal)
   * - HTTP 404: Appeal or user not found
   * - HTTP 500: Unexpected error
   *
   * @param appealId Appeal UUID from path parameter
   * @param authentication Spring Security authentication (contains user email)
   * @return ResponseEntity with AppealResponse (200 OK)
   */
  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<AppealResponse> getAppealDetails(
      @PathVariable("id") UUID appealId,
      Authentication authentication) {

    log.debug("Appeal details request for id: {}", appealId);

    String userEmail = (String) authentication.getPrincipal();
    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

    AppealResponse appeal = appealService.getAppealDetails(appealId);

    // Authorization check: User can only view their own appeal unless ADMIN
    if (!appeal.getUserId().equals(user.getId()) && !user.hasRole(Role.ADMIN)) {
      log.warn("User {} attempted unauthorized access to appeal {}", userEmail, appealId);
      throw new ForbiddenException("You do not have permission to view this appeal");
    }

    log.debug("Appeal details retrieved for id: {}", appealId);
    return ResponseEntity.ok(appeal);
  }

  /**
   * Approve a suspension appeal and release user's suspension.
   *
   * <p><b>Endpoint</b>: POST /api/v1/appeals/{id}/approve
   *
   * <p><b>Authorization</b>: ADMIN only (RBAC)
   *
   * <p><b>Path Parameters</b>:
   * - id: Appeal UUID
   *
   * <p><b>Request</b>: AppealDecisionRequest
   * - decision: Optional admin reason for approval
   *
   * <p><b>Response</b>: AppealResponse (HTTP 200 OK)
   * - status: APPROVED
   * - reviewedByUserEmail: Admin's email
   * - reviewedAt: Review timestamp
   * - decision: Admin's decision notes
   *
   * <p><b>Side Effects</b>:
   * - User's suspendedUntil is cleared (suspension lifted)
   * - User's noShowCount is reset to 0
   * - APPEAL_APPROVED event is emitted (triggers notifications)
   *
   * <p><b>Error Responses</b>:
   * - HTTP 401: Invalid or missing Bearer token
   * - HTTP 403: Non-admin attempting to approve
   * - HTTP 404: Appeal or admin user not found
   * - HTTP 422: Appeal not pending (already reviewed)
   * - HTTP 500: Unexpected error
   *
   * @param appealId Appeal UUID from path parameter
   * @param request AppealDecisionRequest with optional decision reason
   * @param authentication Spring Security authentication (admin's email)
   * @return ResponseEntity with AppealResponse (200 OK)
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AppealResponse> approveAppeal(
      @PathVariable("id") UUID appealId,
      @Valid @RequestBody AppealDecisionRequest request,
      Authentication authentication) {

    log.info("Appeal approval request for id: {}", appealId);

    String adminEmail = (String) authentication.getPrincipal();
    User admin = userRepository.findByEmail(adminEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

    AppealResponse response = appealService.approveAppeal(appealId, admin.getId(), request.getDecision());

    log.info("Appeal approved by admin {}: {}", adminEmail, appealId);
    return ResponseEntity.ok(response);
  }

  /**
   * Reject a suspension appeal (suspension remains).
   *
   * <p><b>Endpoint</b>: POST /api/v1/appeals/{id}/reject
   *
   * <p><b>Authorization</b>: ADMIN only (RBAC)
   *
   * <p><b>Path Parameters</b>:
   * - id: Appeal UUID
   *
   * <p><b>Request</b>: AppealDecisionRequest
   * - decision: Admin reason for rejection (recommended)
   *
   * <p><b>Response</b>: AppealResponse (HTTP 200 OK)
   * - status: REJECTED
   * - reviewedByUserEmail: Admin's email
   * - reviewedAt: Review timestamp
   * - decision: Admin's reason for rejection
   *
   * <p><b>Side Effects</b>:
   * - User's suspension continues (no suspension updates)
   * - APPEAL_REJECTED event is emitted (triggers notifications)
   *
   * <p><b>Error Responses</b>:
   * - HTTP 401: Invalid or missing Bearer token
   * - HTTP 403: Non-admin attempting to reject
   * - HTTP 404: Appeal or admin user not found
   * - HTTP 422: Appeal not pending (already reviewed)
   * - HTTP 500: Unexpected error
   *
   * @param appealId Appeal UUID from path parameter
   * @param request AppealDecisionRequest with optional decision reason
   * @param authentication Spring Security authentication (admin's email)
   * @return ResponseEntity with AppealResponse (200 OK)
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AppealResponse> rejectAppeal(
      @PathVariable("id") UUID appealId,
      @Valid @RequestBody AppealDecisionRequest request,
      Authentication authentication) {

    log.info("Appeal rejection request for id: {}", appealId);

    String adminEmail = (String) authentication.getPrincipal();
    User admin = userRepository.findByEmail(adminEmail)
        .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

    AppealResponse response = appealService.rejectAppeal(appealId, admin.getId(), request.getDecision());

    log.info("Appeal rejected by admin {}: {}", adminEmail, appealId);
    return ResponseEntity.ok(response);
  }
}
