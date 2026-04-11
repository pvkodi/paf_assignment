package com.sliitreserve.api.services.auth;

import com.sliitreserve.api.dto.auth.AppealRequest;
import com.sliitreserve.api.dto.auth.AppealResponse;
import com.sliitreserve.api.entities.auth.SuspensionAppeal;
import com.sliitreserve.api.entities.auth.SuspensionAppealStatus;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventPublisher;
import com.sliitreserve.api.observer.EventSeverity;
import com.sliitreserve.api.repositories.AppealRepository;
import com.sliitreserve.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing suspension appeals and suspension release lifecycle.
 *
 * <p><b>Purpose</b>: Implements FR-023 (user appeals for suspension lift) with approval/rejection
 * workflows. Handles appeal submission, admin review queue, approval (suspend release + no-show
 * reset), and rejection.
 *
 * <p><b>Responsibility</b>:
 * <ul>
 *   <li>Submit appeal (user creates appeal for their suspension)
 *   <li>Get pending appeals (admin views review queue)
 *   <li>Get appeal details and history
 *   <li>Approve appeal (reset suspension and no-show count)
 *   <li>Reject appeal (keep suspension)
 *   <li>Emit events for appeal submission/approval/rejection (for notifications)
 * </ul>
 *
 * <p><b>Functional Requirements</b>:
 * <ul>
 *   <li>FR-003: Suspended users allowed to submit appeals
 *   <li>FR-022: Automatic suspension after 3 no-shows
 *   <li>FR-023: User can appeal suspension; admin reviews and approves (lift suspension + reset
 *       noShowCount) or rejects (keep suspension)
 * </ul>
 *
 * <p><b>Error Handling</b>:
 * <ul>
 *   <li>User is not suspended → ValidationException
 *   <li>Appeal already pending → ConflictException (prevent duplicate submissions)
 *   <li>Invalid appeal ID → ResourceNotFoundException
 *   <li>Unauthorized access → ForbiddenException
 * </ul>
 *
 * <p><b>Integration Points</b>:
 * <ul>
 *   <li>SuspensionPolicyService: Calls releaseSuspension() when appeal approved
 *   <li>UserRepository: Fetches user by email
 *   <li>AppealRepository: CRUD for appeal entities
 *   <li>EventPublisher: Emits APPEAL_SUBMITTED, APPEAL_APPROVED, APPEAL_REJECTED events
 *   <li>AppealController: Delegates from REST endpoints
 * </ul>
 *
 * @see SuspensionAppeal for the appeal entity
 * @see SuspensionPolicyService for suspension management
 * @see AppealController for REST endpoints
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppealService {

  private final AppealRepository appealRepository;
  private final UserRepository userRepository;
  private final SuspensionPolicyService suspensionPolicyService;
  private final EventPublisher eventPublisher;

  /**
   * Submit a new suspension appeal for the current user.
   *
   * <p>Implements FR-023: User can appeal their suspension status.
   * Only suspended users can submit appeals. Prevents duplicate pending appeals.
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Verify user is currently suspended
   *   <li>Check for existing pending appeal (prevent duplicates)
   *   <li>Create appeal with SUBMITTED status
   *   <li>Emit APPEAL_SUBMITTED event for notification
   * </ul>
   *
   * <p><b>Error Cases</b>:
   * <ul>
   *   <li>User not suspended → ValidationException
   *   <li>Pending appeal already exists → ConflictException
   *   <li>User not found → ResourceNotFoundException
   * </ul>
   *
   * @param userId UUID of the user submitting the appeal
   * @param request AppealRequest with appeal reason
   * @return AppealResponse with newly created appeal details
   * @throws ValidationException if user is not suspended
   * @throws ConflictException if user has pending appeal
   * @throws ResourceNotFoundException if user not found
   */
  @Transactional
  public AppealResponse submitAppeal(UUID userId, AppealRequest request) {
    log.info("Processing appeal submission for user: {}", userId);

    // Fetch user
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    // Verify user is suspended
    if (!suspensionPolicyService.isSuspended(user)) {
      log.warn("User {} attempted to appeal while not suspended", user.getEmail());
      throw new ValidationException("You are not currently suspended. Appeals can only be submitted by suspended users.");
    }

    // Check for existing pending appeal (prevent duplicates)
    long pendingCount = appealRepository.countByUserIdAndStatus(userId, SuspensionAppealStatus.SUBMITTED);
    if (pendingCount > 0) {
      log.warn("User {} has existing pending appeal", user.getEmail());
      throw new ConflictException("You already have a pending appeal. Please wait for the review decision.");
    }

    // Create appeal
    SuspensionAppeal appeal = SuspensionAppeal.builder()
        .user(user)
        .reason(request.getReason())
        .status(SuspensionAppealStatus.SUBMITTED)
        .build();

    appeal = appealRepository.save(appeal);
    log.info("Appeal created with id: {} for user: {}", appeal.getId(), user.getEmail());

    // Emit event for notification
    eventPublisher.publish(EventEnvelope.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("APPEAL_SUBMITTED")
        .severity(EventSeverity.HIGH)
        .affectedUserId(user.getId().getMostSignificantBits()) // Convert UUID to Long (use UUID hashCode if needed)
        .title("Suspension Appeal Submitted")
        .description("Your appeal against suspension has been submitted and is awaiting admin review.")
        .source("AppealService")
        .occurrenceTime(ZonedDateTime.now(ZoneId.systemDefault()))
        .entityReference("appeal:" + appeal.getId())
        .actionUrl("/appeals/" + appeal.getId())
        .actionLabel("View Appeal")
        .metadata(Map.of(
            "userId", user.getId().toString(),
            "suspensionUntil", user.getSuspendedUntil() != null ? user.getSuspendedUntil().toString() : null,
            "reason", request.getReason()
        ))
        .build());

    return toAppealResponse(appeal);
  }

  /**
   * Get all pending suspension appeals (admin review queue).
   *
   * <p>Implements admin review workflow. Returns appeals ordered by submission time (oldest first)
   * for FIFO processing.
   *
   * @return List of pending appeals ordered by createdAt ascending
   */
  @Transactional(readOnly = true)
  public List<AppealResponse> getPendingAppeals() {
    log.debug("Retrieving pending appeals");

    List<SuspensionAppeal> appeals = appealRepository.findByStatusOrderByCreatedAtAsc(SuspensionAppealStatus.SUBMITTED);

    return appeals.stream()
        .map(this::toAppealResponse)
        .toList();
  }

  /**
   * Get all appeals for a specific user (user's appeal history).
   *
   * <p>Users can view their own appeal history including submitted, approved, and rejected appeals.
   *
   * @param userId UUID of the user
   * @return List of user's appeals ordered by most recent first
   */
  @Transactional(readOnly = true)
  public List<AppealResponse> getUserAppeals(UUID userId) {
    log.debug("Retrieving appeals for user: {}", userId);

    List<SuspensionAppeal> appeals = appealRepository.findByUserIdOrderByCreatedAtDesc(userId);

    return appeals.stream()
        .map(this::toAppealResponse)
        .toList();
  }

  /**
   * Get appeal details by ID.
   *
   * <p>Returns appeal with all metadata including review decision and admin notes.
   *
   * @param appealId UUID of the appeal
   * @return AppealResponse with appeal details
   * @throws ResourceNotFoundException if appeal not found
   */
  @Transactional(readOnly = true)
  public AppealResponse getAppealDetails(UUID appealId) {
    log.debug("Retrieving appeal details: {}", appealId);

    SuspensionAppeal appeal = appealRepository.findById(appealId)
        .orElseThrow(() -> new ResourceNotFoundException("Appeal not found with id: " + appealId));

    return toAppealResponse(appeal);
  }

  /**
   * Approve a suspension appeal and release user's suspension.
   *
   * <p>Implements FR-023: Admin can approve appeal → suspension lifted + noShowCount reset.
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Find appeal by ID
   *   <li>Verify appeal is in SUBMITTED status
   *   <li>Update appeal status to APPROVED
   *   <li>Call SuspensionPolicyService.releaseSuspension() to clear suspension
   *   <li>Store admin decision and review timestamp
   *   <li>Emit APPEAL_APPROVED event for notification
   * </ul>
   *
   * <p><b>Error Cases</b>:
   * <ul>
   *   <li>Appeal not found → ResourceNotFoundException
   *   <li>Appeal already reviewed → ValidationException
   * </ul>
   *
   * @param appealId UUID of the appeal to approve
   * @param reviewedByUserId UUID of the admin approving
   * @param decision Optional admin reason for approval
   * @return AppealResponse with updated appeal details
   * @throws ResourceNotFoundException if appeal not found
   * @throws ValidationException if appeal not pending
   */
  @Transactional
  public AppealResponse approveAppeal(UUID appealId, UUID reviewedByUserId, String decision) {
    log.info("Processing appeal approval: {}", appealId);

    // Fetch appeal
    SuspensionAppeal appeal = appealRepository.findById(appealId)
        .orElseThrow(() -> new ResourceNotFoundException("Appeal not found with id: " + appealId));

    // Verify appeal is pending
    if (!appeal.isPending()) {
      log.warn("Attempted to approve already-reviewed appeal: {}", appealId);
      throw new ValidationException("Appeal has already been reviewed and cannot be changed.");
    }

    // Fetch reviewer
    User reviewer = userRepository.findById(reviewedByUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with id: " + reviewedByUserId));

    // Update appeal status and review metadata
    appeal.setStatus(SuspensionAppealStatus.APPROVED);
    appeal.setReviewedByUser(reviewer);
    appeal.setReviewedAt(LocalDateTime.now(ZoneId.systemDefault()));
    appeal.setDecision(decision);

    appeal = appealRepository.save(appeal);
    log.info("Appeal approved: {}", appealId);

    // Release user's suspension
    User user = appeal.getUser();
    suspensionPolicyService.releaseSuspension(user);
    log.info("Suspension released for user: {}", user.getEmail());

    // Emit APPROVED event for notification
    eventPublisher.publish(EventEnvelope.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("APPEAL_APPROVED")
        .severity(EventSeverity.HIGH)
        .affectedUserId(user.getId().getMostSignificantBits())
        .title("Suspension Appeal Approved")
        .description("Your suspension appeal has been approved. Your account suspension has been lifted.")
        .source("AppealService")
        .occurrenceTime(ZonedDateTime.now(ZoneId.systemDefault()))
        .entityReference("appeal:" + appeal.getId())
        .actionUrl("/profile")
        .actionLabel("View Profile")
        .metadata(Map.of(
            "userId", user.getId().toString(),
            "reviewedBy", reviewer.getEmail(),
            "decision", decision != null ? decision : ""
        ))
        .build());

    return toAppealResponse(appeal);
  }

  /**
   * Reject a suspension appeal (suspension remains).
   *
   * <p>Implements FR-023: Admin can reject appeal → suspension continues.
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Find appeal by ID
   *   <li>Verify appeal is in SUBMITTED status
   *   <li>Update appeal status to REJECTED
   *   <li>Store admin decision and review timestamp
   *   <li>Emit APPEAL_REJECTED event for notification
   * </ul>
   *
   * <p><b>Error Cases</b>:
   * <ul>
   *   <li>Appeal not found → ResourceNotFoundException
   *   <li>Appeal already reviewed → ValidationException
   * </ul>
   *
   * @param appealId UUID of the appeal to reject
   * @param reviewedByUserId UUID of the admin rejecting
   * @param decision Admin reason for rejection
   * @return AppealResponse with updated appeal details
   * @throws ResourceNotFoundException if appeal not found
   * @throws ValidationException if appeal not pending
   */
  @Transactional
  public AppealResponse rejectAppeal(UUID appealId, UUID reviewedByUserId, String decision) {
    log.info("Processing appeal rejection: {}", appealId);

    // Fetch appeal
    SuspensionAppeal appeal = appealRepository.findById(appealId)
        .orElseThrow(() -> new ResourceNotFoundException("Appeal not found with id: " + appealId));

    // Verify appeal is pending
    if (!appeal.isPending()) {
      log.warn("Attempted to reject already-reviewed appeal: {}", appealId);
      throw new ValidationException("Appeal has already been reviewed and cannot be changed.");
    }

    // Fetch reviewer
    User reviewer = userRepository.findById(reviewedByUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found with id: " + reviewedByUserId));

    // Update appeal status and review metadata
    appeal.setStatus(SuspensionAppealStatus.REJECTED);
    appeal.setReviewedByUser(reviewer);
    appeal.setReviewedAt(LocalDateTime.now(ZoneId.systemDefault()));
    appeal.setDecision(decision);

    appeal = appealRepository.save(appeal);
    log.info("Appeal rejected: {}", appealId);

    // Note: User's suspension continues (no action taken on suspension)

    // Emit REJECTED event for notification
    User user = appeal.getUser();
    eventPublisher.publish(EventEnvelope.builder()
        .eventId(UUID.randomUUID().toString())
        .eventType("APPEAL_REJECTED")
        .severity(EventSeverity.HIGH)
        .affectedUserId(user.getId().getMostSignificantBits())
        .title("Suspension Appeal Rejected")
        .description("Your suspension appeal has been rejected. Your account suspension remains in effect.")
        .source("AppealService")
        .occurrenceTime(ZonedDateTime.now(ZoneId.systemDefault()))
        .entityReference("appeal:" + appeal.getId())
        .actionUrl("/appeals/" + appeal.getId())
        .actionLabel("View Decision")
        .metadata(Map.of(
            "userId", user.getId().toString(),
            "reviewedBy", reviewer.getEmail(),
            "suspensionUntil", user.getSuspendedUntil() != null ? user.getSuspendedUntil().toString() : null,
            "decision", decision != null ? decision : ""
        ))
        .build());

    return toAppealResponse(appeal);
  }

  /**
   * Convert SuspensionAppeal entity to AppealResponse DTO.
   *
   * @param appeal SuspensionAppeal entity
   * @return AppealResponse DTO
   */
  private AppealResponse toAppealResponse(SuspensionAppeal appeal) {
    String reviewerEmail = appeal.getReviewedByUser() != null ? appeal.getReviewedByUser().getEmail() : null;

    return new AppealResponse(
        appeal.getId(),
        appeal.getUser().getId(),
        appeal.getUser().getEmail(),
        appeal.getReason(),
        appeal.getStatus(),
        reviewerEmail,
        appeal.getReviewedAt(),
        appeal.getDecision(),
        appeal.getCreatedAt(),
        appeal.getUpdatedAt()
    );
  }
}
