package com.sliitreserve.api.services.auth;

import com.sliitreserve.api.dto.auth.CreateRegistrationRequestDTO;
import com.sliitreserve.api.dto.auth.RegistrationRequestDTO;
import com.sliitreserve.api.entities.auth.RegistrationRequest;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.repositories.auth.RegistrationRequestRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user registration requests.
 *
 * <p>Handles the registration approval workflow:
 * <ul>
 *   <li>Create registration requests (user submits)</li>
 *   <li>Fetch requests by status for admin panel</li>
 *   <li>Approve requests (admin action - creates User and publishes approval event)</li>
 *   <li>Reject requests (admin action - publishes rejection event with reason)</li>
 * </ul>
 *
 * <p><b>Registration Approval Flow</b>:
 * <ol>
 *   <li>User calls createRegistrationRequest() with credentials and role</li>
 *   <li>RegistrationRequest is saved with status=PENDING</li>
 *   <li>Admin calls approveRequest() or rejectRequest()</li>
 *   <li>If approved: User entity created, status=APPROVED, approval event published (HIGH severity)</li>
 *   <li>If rejected: status=REJECTED, rejection event published (HIGH severity) with reason</li>
 *   <li>User receives email notification of decision</li>
 * </ol>
 *
 * <p><b>Security</b>:
 * <ul>
 *   <li>Passwords are BCrypt hashed before storage in RegistrationRequest</li>
 *   <li>Passwords are never included in DTOs sent to frontend</li>
 *   <li>Only ADMIN role can approve/reject requests</li>
 *   <li>Duplicate pending emails are rejected</li>
 * </ul>
 */
@Service
@Slf4j
public class RegistrationRequestService {

  @Autowired
  private RegistrationRequestRepository registrationRequestRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EventPublisher eventPublisher;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

  /**
   * Create a new registration request.
   *
   * <p>Validates:
   * <ul>
   *   <li>Email is not already registered as User</li>
   *   <li>Email doesn't have a PENDING registration request</li>
   *   <li>Passwords match</li>
   *   <li>Role-specific credentials are provided</li>
   * </ul>
   *
   * @param request Registration request with credentials and role
   * @return DTO representation of the created request
   * @throws IllegalArgumentException if validation fails
   */
  @Transactional
  public RegistrationRequestDTO createRegistrationRequest(CreateRegistrationRequestDTO request) {
    log.info("Creating registration request for email: {}", request.getEmail());

    // Validate passwords match
    request.validatePasswords();

    // Validate role-specific credentials
    request.validateRoleCredentials();

    // Check if email already registered as User
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      log.warn("Registration failed: email already registered as user: {}", request.getEmail());
      throw new IllegalArgumentException("Email already registered");
    }

    // Check if email has pending registration request
    if (registrationRequestRepository.existsByEmailAndPendingStatus(request.getEmail())) {
      log.warn(
          "Registration failed: pending request already exists for email: {}", request.getEmail());
      throw new IllegalArgumentException(
          "A registration request is already pending for this email. Please wait for admin approval.");
    }

    // Hash password
    String passwordHash = passwordEncoder.encode(request.getPassword());

    // Create and save registration request
    RegistrationRequest regRequest =
        RegistrationRequest.builder()
            .email(request.getEmail())
            .displayName(request.getDisplayName())
            .passwordHash(passwordHash)
            .roleRequested(request.getRoleRequested())
            .registrationNumber(request.getRegistrationNumber())
            .employeeNumber(request.getEmployeeNumber())
            .status(RegistrationRequest.RegistrationRequestStatus.PENDING)
            .build();

    regRequest = registrationRequestRepository.save(regRequest);
    log.info("Registration request created: {} (status: {})", regRequest.getId(), regRequest.getStatus());

    return RegistrationRequestDTO.fromEntity(regRequest);
  }

  /**
   * Get all pending registration requests, paginated.
   *
   * @param pageable Pagination parameters
   * @return Page of pending registration requests
   */
  @Transactional(readOnly = true)
  public Page<RegistrationRequestDTO> getPendingRequests(Pageable pageable) {
    return registrationRequestRepository
        .findAllPending(pageable)
        .map(RegistrationRequestDTO::fromEntity);
  }

  /**
   * Get registration requests by status, paginated.
   *
   * @param status The status to filter by (PENDING, APPROVED, REJECTED)
   * @param pageable Pagination parameters
   * @return Page of matching registration requests
   */
  @Transactional(readOnly = true)
  public Page<RegistrationRequestDTO> getRequestsByStatus(
      RegistrationRequest.RegistrationRequestStatus status, Pageable pageable) {
    return registrationRequestRepository
        .findByStatus(status, pageable)
        .map(RegistrationRequestDTO::fromEntity);
  }

  /**
   * Get a single registration request by ID.
   *
   * @param id Registration request ID
   * @return DTO representation, or empty if not found
   */
  @Transactional(readOnly = true)
  public Optional<RegistrationRequestDTO> getRequestById(UUID id) {
    return registrationRequestRepository.findById(id).map(RegistrationRequestDTO::fromEntity);
  }

  /**
   * Approve a registration request.
   *
   * <p>Flow:
   * <ol>
   *   <li>Fetch registration request</li>
   *   <li>Verify status is PENDING</li>
   *   <li>Create User entity from request</li>
   *   <li>Update registration request status to APPROVED</li>
   *   <li>Publish HIGH severity approval event (triggers email)</li>
   * </ol>
   *
   * @param requestId ID of the registration request
   * @param adminId UUID of admin approving
   * @param approvalNote Optional admin note
   * @return DTO representation of approved request
   * @throws IllegalArgumentException if request not found or not PENDING
   */
  @Transactional
  public RegistrationRequestDTO approveRequest(UUID requestId, UUID adminId, String approvalNote) {
    log.info("Admin {} approving registration request: {}", adminId, requestId);

    RegistrationRequest regRequest =
        registrationRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> {
                  log.error("Registration request not found: {}", requestId);
                  return new IllegalArgumentException("Registration request not found");
                });

    if (!regRequest.getStatus().equals(RegistrationRequest.RegistrationRequestStatus.PENDING)) {
      log.warn(
          "Cannot approve registration request {}: status is {}", requestId, regRequest.getStatus());
      throw new IllegalArgumentException(
          "Only PENDING requests can be approved. Current status: " + regRequest.getStatus());
    }

    // Check if email now conflicts with existing user
    if (userRepository.findByEmail(regRequest.getEmail()).isPresent()) {
      log.warn(
          "Cannot approve registration request {}: email already registered: {}",
          requestId,
          regRequest.getEmail());
      throw new IllegalArgumentException(
          "Email already registered as user. Registration request cannot be approved.");
    }

    // Create User entity from registration request
    User newUser =
        User.builder()
            .email(regRequest.getEmail())
            .displayName(regRequest.getDisplayName())
            .passwordHash(regRequest.getPasswordHash()) // Use hashed password from request
            .roles(Collections.singleton(regRequest.getRoleRequested()))
            .active(true)
            .noShowCount(0)
            .suspendedUntil(null)
            .build();

    newUser = userRepository.save(newUser);
    log.info(
        "User created from approved registration request: {} ({})",
        newUser.getId(),
        newUser.getEmail());

    // Update registration request status to APPROVED
    regRequest.setStatus(RegistrationRequest.RegistrationRequestStatus.APPROVED);
    regRequest.setReviewedByAdminId(adminId);
    regRequest.setReviewedAt(LocalDateTime.now());
    regRequest = registrationRequestRepository.save(regRequest);
    log.info("Registration request {} approved by admin {}", requestId, adminId);

    // Publish approval event (HIGH severity for email notification)
    eventPublisher.publish(
        EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("USER_REGISTRATION_APPROVED")
            .severity(EventSeverity.HIGH)
            .affectedUserId(newUser.getId().getMostSignificantBits())
            .title("Your Registration Has Been Approved")
            .description(
                "Congratulations! Your registration has been approved. You can now log in to your account.")
            .source("RegistrationService")
            .entityReference("user:" + newUser.getId())
            .actionUrl("/dashboard")
            .actionLabel("Go to Dashboard")
            .occurrenceTime(ZonedDateTime.now())
            .metadata(
                Map.of(
                    "userId", newUser.getId().toString(),
                    "email", newUser.getEmail(),
                    "role", newUser.getRoles().iterator().next().toString()))
            .build());

    return RegistrationRequestDTO.fromEntity(regRequest);
  }

  /**
   * Reject a registration request.
   *
   * <p>Flow:
   * <ol>
   *   <li>Fetch registration request</li>
   *   <li>Verify status is PENDING</li>
   *   <li>Update status to REJECTED with reason</li>
   *   <li>Publish HIGH severity rejection event (triggers email with reason)</li>
   *   <li>User can resubmit new registration request</li>
   * </ol>
   *
   * @param requestId ID of the registration request
   * @param adminId UUID of admin rejecting
   * @param rejectionReason Reason for rejection (sent to user)
   * @return DTO representation of rejected request
   * @throws IllegalArgumentException if request not found or not PENDING
   */
  @Transactional
  public RegistrationRequestDTO rejectRequest(
      UUID requestId, UUID adminId, String rejectionReason) {
    log.info("Admin {} rejecting registration request: {}", adminId, requestId);

    RegistrationRequest regRequest =
        registrationRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> {
                  log.error("Registration request not found: {}", requestId);
                  return new IllegalArgumentException("Registration request not found");
                });

    if (!regRequest.getStatus().equals(RegistrationRequest.RegistrationRequestStatus.PENDING)) {
      log.warn(
          "Cannot reject registration request {}: status is {}", requestId, regRequest.getStatus());
      throw new IllegalArgumentException(
          "Only PENDING requests can be rejected. Current status: " + regRequest.getStatus());
    }

    // Update registration request status to REJECTED
    regRequest.setStatus(RegistrationRequest.RegistrationRequestStatus.REJECTED);
    regRequest.setRejectionReason(rejectionReason);
    regRequest.setReviewedByAdminId(adminId);
    regRequest.setReviewedAt(LocalDateTime.now());
    regRequest = registrationRequestRepository.save(regRequest);
    log.info(
        "Registration request {} rejected by admin {} with reason: {}",
        requestId,
        adminId,
        rejectionReason);

    // Publish rejection event (HIGH severity for email notification)
    eventPublisher.publish(
        EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("USER_REGISTRATION_REJECTED")
            .severity(EventSeverity.HIGH)
            .affectedUserId(regRequest.getId().getMostSignificantBits())
            .title("Your Registration Has Been Rejected")
            .description(
                "Your registration has been reviewed and rejected. Reason: "
                    + rejectionReason
                    + "\n\nYou can submit a new registration request.")
            .source("RegistrationService")
            .entityReference("registration_request:" + regRequest.getId())
            .actionUrl("/register")
            .actionLabel("Submit New Registration")
            .occurrenceTime(ZonedDateTime.now())
            .metadata(
                Map.of(
                    "email", regRequest.getEmail(),
                    "rejectionReason", rejectionReason))
            .build());

    return RegistrationRequestDTO.fromEntity(regRequest);
  }
}
