package com.sliitreserve.api.controllers.admin;

import com.sliitreserve.api.dto.auth.ApproveRegistrationRequestDTO;
import com.sliitreserve.api.dto.auth.RegistrationRequestDTO;
import com.sliitreserve.api.dto.auth.RejectRegistrationRequestDTO;
import com.sliitreserve.api.entities.auth.RegistrationRequest;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.services.auth.RegistrationRequestService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin User Management Controller
 *
 * <p>Endpoints for admin to manage user registration requests and user data.
 * Base path: /api/v1/admin/user-management
 *
 * <p><b>Access Control</b>:
 * <ul>
 *   <li>All endpoints require ADMIN role</li>
 *   <li>Enforced via @PreAuthorize("hasRole('ADMIN')")</li>
 * </ul>
 *
 * <p><b>Endpoints</b>:
 * <ul>
 *   <li>GET /registration-requests - List registration requests with filtering and pagination</li>
 *   <li>GET /registration-requests/{id} - Get single registration request details</li>
 *   <li>POST /registration-requests/{id}/approve - Approve a registration request</li>
 *   <li>POST /registration-requests/{id}/reject - Reject a registration request</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/user-management")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

  @Autowired
  private RegistrationRequestService registrationRequestService;

  @Autowired
  private com.sliitreserve.api.repositories.auth.UserRepository userRepository;

  /**
   * Get registration requests with filtering and pagination.
   *
   * <p>Query parameters:
   * <ul>
   *   <li>status: PENDING, APPROVED, or REJECTED (defaults to PENDING if not specified)</li>
   *   <li>page: 0-based page number (default: 0)</li>
   *   <li>size: page size (default: 20)</li>
   *   <li>sort: sort field and direction (default: createdAt,desc)</li>
   * </ul>
   *
   * @param status Registration request status filter (optional)
   * @param pageable Pagination parameters
   * @return Page of registration requests
   */
  @GetMapping("/registration-requests")
  public ResponseEntity<?> getRegistrationRequests(
      @RequestParam(required = false) RegistrationRequest.RegistrationRequestStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    log.info("Admin fetching registration requests with status: {}", status);

    try {
      Page<RegistrationRequestDTO> requests;

      if (status == null) {
        // Default to PENDING if no status specified
        requests = registrationRequestService.getPendingRequests(pageable);
      } else {
        requests = registrationRequestService.getRequestsByStatus(status, pageable);
      }

      log.debug("Retrieved {} registration requests for admin", requests.getNumberOfElements());

      return ResponseEntity.ok(requests);

    } catch (Exception e) {
      log.error("Error fetching registration requests: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("FETCH_ERROR", "Failed to fetch registration requests"));
    }
  }

  /**
   * Get a single registration request by ID.
   *
   * @param id Registration request ID
   * @return Registration request details
   */
  @GetMapping("/registration-requests/{id}")
  public ResponseEntity<?> getRegistrationRequest(@PathVariable UUID id) {
    log.info("Admin fetching registration request: {}", id);

    try {
      Optional<RegistrationRequestDTO> request = registrationRequestService.getRequestById(id);

      if (request.isEmpty()) {
        log.warn("Registration request not found: {}", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse("NOT_FOUND", "Registration request not found"));
      }

      return ResponseEntity.ok(request.get());

    } catch (Exception e) {
      log.error("Error fetching registration request: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("FETCH_ERROR", "Failed to fetch registration request"));
    }
  }

  /**
   * Approve a registration request.
   *
   * <p>Flow:
   * <ol>
   *   <li>Fetch registration request (must be PENDING)</li>
   *   <li>Create User entity from request</li>
   *   <li>Update request status to APPROVED</li>
   *   <li>Publish approval event (triggers email)</li>
   * </ol>
   *
   * @param id Registration request ID
   * @param approveDto Admin approval details (optional note)
   * @return Approved registration request
   */
  @PostMapping("/registration-requests/{id}/approve")
  public ResponseEntity<?> approveRegistrationRequest(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) ApproveRegistrationRequestDTO approveDto) {
    log.info("Admin approving registration request: {}", id);

    try {
      String adminId = getAdminIdFromContext();
      String approvalNote = approveDto != null ? approveDto.getNote() : null;

      RegistrationRequestDTO approvedRequest =
          registrationRequestService.approveRequest(id, UUID.fromString(adminId), approvalNote);

      log.info("Registration request approved: {} by admin: {}", id, adminId);

      return ResponseEntity.ok(
          Map.of(
              "message", "Registration request approved successfully",
              "request", approvedRequest));

    } catch (IllegalArgumentException e) {
      log.warn("Invalid approval request: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));

    } catch (Exception e) {
      log.error("Error approving registration request: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("APPROVAL_ERROR", "Failed to approve registration request"));
    }
  }

  /**
   * Reject a registration request.
   *
   * <p>Flow:
   * <ol>
   *   <li>Fetch registration request (must be PENDING)</li>
   *   <li>Update request status to REJECTED with reason</li>
   *   <li>Publish rejection event (triggers email with reason)</li>
   * </ol>
   *
   * @param id Registration request ID
   * @param rejectDto Rejection details (reason required)
   * @return Rejected registration request
   */
  @PostMapping("/registration-requests/{id}/reject")
  public ResponseEntity<?> rejectRegistrationRequest(
      @PathVariable UUID id,
      @Valid @RequestBody RejectRegistrationRequestDTO rejectDto) {
    log.info("Admin rejecting registration request: {}", id);

    try {
      String adminId = getAdminIdFromContext();

      RegistrationRequestDTO rejectedRequest =
          registrationRequestService.rejectRequest(id, UUID.fromString(adminId), rejectDto.getReason());

      log.info("Registration request rejected: {} by admin: {}", id, adminId);

      return ResponseEntity.ok(
          Map.of(
              "message", "Registration request rejected successfully",
              "request", rejectedRequest));

    } catch (IllegalArgumentException e) {
      log.warn("Invalid rejection request: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));

    } catch (Exception e) {
      log.error("Error rejecting registration request: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("REJECTION_ERROR", "Failed to reject registration request"));
    }
  }

  /**
   * Get all users with filtering and pagination.
   *
   * <p>Query parameters:
   * <ul>
   *   <li>query: Search by email or display name (optional)</li>
   *   <li>role: Filter by role - can be repeated for multiple roles (optional)</li>
   *   <li>page: 0-based page number (default: 0)</li>
   *   <li>size: page size (default: 20)</li>
   *   <li>sort: sort field and direction (default: createdAt,desc)</li>
   * </ul>
   *
   * @param query Search query for email or display name (optional)
   * @param role List of roles to filter by (optional)
   * @param pageable Pagination parameters
   * @return Page of users
   */
  @GetMapping("/users")
  public ResponseEntity<?> getAllUsers(
      @RequestParam(required = false) String query,
      @RequestParam(required = false, name = "role") java.util.List<String> role,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    log.info("Admin fetching users - query: {}, roles: {}", query, role);

    try {
      java.util.List<com.sliitreserve.api.entities.auth.User> users;

      // Search by email or display name if query provided
      if (query != null && !query.trim().isEmpty()) {
        users = userRepository.searchByEmailOrDisplayName(query.trim(), query.trim());
      } else {
        users = userRepository.findAll();
      }

      // Filter by roles if provided
      if (role != null && !role.isEmpty()) {
        users = users.stream()
            .filter(user -> user.getRoles() != null && user.getRoles().stream()
                .anyMatch(userRole -> role.contains(userRole.name())))
            .toList();
      }

      // Manual pagination since we filtered in memory
      int start = (int) pageable.getOffset();
      int end = Math.min(start + pageable.getPageSize(), users.size());
      java.util.List<com.sliitreserve.api.dto.auth.UserDTO> pagedUsers = users.subList(start, end)
          .stream()
          .map(user -> com.sliitreserve.api.dto.auth.UserDTO.fromEntity(user))
          .toList();

      org.springframework.data.domain.Page<com.sliitreserve.api.dto.auth.UserDTO> page =
          new org.springframework.data.domain.PageImpl<>(
              pagedUsers,
              pageable,
              users.size());

      log.debug("Retrieved {} users for admin", page.getNumberOfElements());

      return ResponseEntity.ok(page);

    } catch (Exception e) {
      log.error("Error fetching users: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("FETCH_ERROR", "Failed to fetch users"));
    }
  }

  /**
   * Get a single user by ID.
   *
   * @param id User ID
   * @return User details
   */
  @GetMapping("/users/{id}")
  public ResponseEntity<?> getUserById(@PathVariable UUID id) {
    log.info("Admin fetching user: {}", id);

    try {
      var userOpt = userRepository.findById(id);
      if (userOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse("NOT_FOUND", "User not found"));
      }

      return ResponseEntity.ok(com.sliitreserve.api.dto.auth.UserDTO.fromEntity(userOpt.get()));

    } catch (Exception e) {
      log.error("Error fetching user: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("FETCH_ERROR", "Failed to fetch user"));
    }
  }

  /**
   * Update user details.
   *
   * @param id User ID
   * @param updateDto User update DTO
   * @return Updated user
   */
  @PutMapping("/users/{id}")
  public ResponseEntity<?> updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody com.sliitreserve.api.dto.auth.UpdateUserDTO updateDto) {
    log.info("Admin updating user: {}", id);

    try {
      var userOpt = userRepository.findById(id);
      if (userOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse("NOT_FOUND", "User not found"));
      }

      com.sliitreserve.api.entities.auth.User user = userOpt.get();

      // Update fields
      if (updateDto.getDisplayName() != null && !updateDto.getDisplayName().isBlank()) {
        user.setDisplayName(updateDto.getDisplayName());
      }

      if (updateDto.getEmail() != null && !updateDto.getEmail().isBlank()) {
        user.setEmail(updateDto.getEmail());
      }

      if (updateDto.getSuspendedUntil() != null) {
        user.setSuspendedUntil(updateDto.getSuspendedUntil());
      }

      if (updateDto.getNoShowCount() != null) {
        user.setNoShowCount(updateDto.getNoShowCount());
      }

      user.setUpdatedAt(java.time.LocalDateTime.now());
      com.sliitreserve.api.entities.auth.User updated = userRepository.save(user);

      log.info("User updated: {}", id);

      return ResponseEntity.ok(
          Map.of(
              "message", "User updated successfully",
              "user", com.sliitreserve.api.dto.auth.UserDTO.fromEntity(updated)));

    } catch (Exception e) {
      log.error("Error updating user: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(createErrorResponse("UPDATE_ERROR", "Failed to update user"));
    }
  }

  /**
   * Extract admin ID from security context.
   * Retrieves the user email from the authentication principal and looks up the user ID.
   *
   * @return Admin UUID as string
   * @throws ForbiddenException if admin ID cannot be extracted
   */
  private String getAdminIdFromContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("No authenticated user found in security context");
      throw new ForbiddenException("Not authenticated");
    }

    // Principal is the email (set by JwtAuthenticationFilter)
    Object principal = authentication.getPrincipal();
    
    if (principal == null || !(principal instanceof String)) {
      log.warn("Principal is not a String email");
      throw new ForbiddenException("Invalid authentication principal");
    }

    String email = (String) principal;
    
    // Find user by email to get their ID
    var userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
      log.warn("User not found for email: {}", email);
      throw new ForbiddenException("User not found");
    }

    String userId = userOpt.get().getId().toString();
    log.debug("Extracted admin ID from email {}: {}", email, userId);
    return userId;
  }

  private Map<String, Object> createErrorResponse(String errorCode, String message) {
    return Map.of("errorCode", errorCode, "message", message);
  }
}
