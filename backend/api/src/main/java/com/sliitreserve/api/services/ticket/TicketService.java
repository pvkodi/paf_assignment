package com.sliitreserve.api.services.ticket;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.ticket.*;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.ticket.TicketCommentRepository;
import com.sliitreserve.api.state.TicketStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing maintenance tickets and comments with visibility-based access control.
 *
 * <p><b>Purpose</b>: Orchestrate ticket lifecycle (creation, updates, status transitions),
 * comment management with role-based visibility filtering, and ticket assignment. Enforces
 * ticket state machine transitions and implements comment visibility rules (FR-025, FR-026, FR-029).
 *
 * <p><b>Features</b>:
 * <ul>
 *   <li>Ticket creation with SLA deadline calculation
 *   <li>Ticket status transitions with state machine validation
 *   <li>Ticket assignment to technicians
 *   <li>Comment management with soft-delete support
 *   <li>Role-based comment visibility filtering (PUBLIC vs INTERNAL)
 *   <li>Comment author edit/delete permissions
 *   <li>Ticket retrieval with permission checks
 * </ul>
 *
 * <p><b>Comment Visibility Rules</b> (FR-026, FR-029):
 * <ul>
 *   <li><b>PUBLIC comments</b>: Visible to ticket creator, assigned technician, and all staff
 *       (TECHNICIAN, FACILITY_MANAGER, ADMIN)
 *   <li><b>INTERNAL comments</b>: Visible to staff only (TECHNICIAN, FACILITY_MANAGER, ADMIN)
 *   <li><b>Comment edit/delete</b>: Authors can modify their own comments; ADMIN can delete any
 *   <li><b>Soft-delete</b>: Deletions set deletedAt timestamp rather then removing records
 * </ul>
 *
 * <p><b>Ticket Access Control</b>:
 * <ul>
 *   <li>Ticket creator and assigned technician can view ticket
 *   <li>All staff (TECHNICIAN, FACILITY_MANAGER, ADMIN) can view all tickets
 *   <li>Permissions enforced at service level, detailed access control at controller
 * </ul>
 *
 * @see MaintenanceTicket
 * @see TicketComment
 * @see TicketStateMachine
 * @author Maintenance Ticketing Module
 */
@Slf4j
@Service
@Transactional
public class TicketService {

  private final MaintenanceTicketRepository ticketRepository;
  private final TicketCommentRepository commentRepository;
  private final TicketStateMachine ticketStateMachine;

  @Autowired
  public TicketService(
      MaintenanceTicketRepository ticketRepository,
      TicketCommentRepository commentRepository,
      TicketStateMachine ticketStateMachine) {
    this.ticketRepository = ticketRepository;
    this.commentRepository = commentRepository;
    this.ticketStateMachine = ticketStateMachine;
  }

  /**
   * Create a new maintenance ticket with SLA deadline calculation.
   *
   * <p>SLA deadline is priority-based (CRITICAL 4h, HIGH 8h, MEDIUM 24h, LOW 72h). Status is initialized to OPEN.
   *
   * @param facility the facility affected by the maintenance issue (must be non-null)
   * @param category the issue category (must be non-null)
   * @param priority the issue priority (must be non-null)
   * @param title the ticket title (20-200 chars)
   * @param description the detailed description of the issue (min 50 chars)
   * @param createdBy the user reporting the issue (must be non-null)
   * @return the created MaintenanceTicket with priority-based SLA deadline
   * @throws IllegalArgumentException if any required parameter is null
   */
  public MaintenanceTicket createTicket(
      Facility facility,
      TicketCategory category,
      TicketPriority priority,
      String title,
      String description,
      User createdBy) {
    // Validation
    if (facility == null) {
      throw new IllegalArgumentException("Facility cannot be null");
    }
    if (category == null) {
      throw new IllegalArgumentException("Category cannot be null");
    }
    if (priority == null) {
      throw new IllegalArgumentException("Priority cannot be null");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Title cannot be null or blank");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Description cannot be null or blank");
    }
    if (createdBy == null) {
      throw new IllegalArgumentException("Created by user cannot be null");
    }

    // Calculate SLA deadline based on priority (24x7 elapsed time in campus timezone)
    LocalDateTime slaDueAt = LocalDateTime.now().plus(
        switch(priority) {
            case CRITICAL -> java.time.Duration.ofHours(4);
            case HIGH -> java.time.Duration.ofHours(8);
            case MEDIUM -> java.time.Duration.ofHours(24);
            case LOW -> java.time.Duration.ofHours(72);
            default -> java.time.Duration.ofHours(48);
        }
    );

    MaintenanceTicket ticket =
        MaintenanceTicket.builder()
            .facility(facility)
            .category(category)
            .priority(priority)
            .title(title)
            .description(description)
            .createdBy(createdBy)
            .status(TicketStatus.OPEN)
            .escalationLevel(0)
            .slaDueAt(slaDueAt)
            .build();

    MaintenanceTicket savedTicket = ticketRepository.save(ticket);
    log.info(
        "Ticket created: ticketId={}, facility={}, priority={}, slaDueAt={}",
        savedTicket.getId(),
        facility.getId(),
        priority,
        LocalDateTime.now());

    return savedTicket;
  }

  /**
   * Retrieve a ticket by ID with full entity initialization.
   *
   * @param ticketId the ticket UUID (must be non-null)
   * @return Optional containing the MaintenanceTicket if found
   * @throws IllegalArgumentException if ticketId is null
   */
  @Transactional(readOnly = true)
  public Optional<MaintenanceTicket> getTicketById(UUID ticketId) {
    if (ticketId == null) {
      throw new IllegalArgumentException("Ticket ID cannot be null");
    }
    return ticketRepository.findById(ticketId);
  }

  /**
   * Retrieve all tickets for a facility.
   *
   * @param facilityId the facility UUID (must be non-null)
   * @return list of MaintenanceTickets for the facility (empty if none)
   * @throws IllegalArgumentException if facilityId is null
   */
  @Transactional(readOnly = true)
  public List<MaintenanceTicket> getTicketsForFacility(UUID facilityId) {
    if (facilityId == null) {
      throw new IllegalArgumentException("Facility ID cannot be null");
    }
    return ticketRepository.findByFacilityId(facilityId);
  }

  /**
   * Retrieve all non-terminal tickets with breached SLA deadlines.
   *
   * <p>Used by escalation scheduler to find tickets ready for automatic escalation.
   *
   * @return list of tickets in non-terminal states with slaDueAt before now
   */
  @Transactional(readOnly = true)
  public List<MaintenanceTicket> getTicketsWithBreachedSla() {
    return ticketRepository.findByStatusNotInAndSlaDueAtBefore(
        Arrays.asList(TicketStatus.CLOSED, TicketStatus.REJECTED), LocalDateTime.now());
  }

  /**
   * Retrieve tickets at a specific escalation level with breached SLA.
   *
   * <p>Used by escalation handlers to process tickets at specific escalation levels.
   *
   * @param escalationLevel the escalation level (0, 1, 2, or 3)
   * @return list of breached tickets at the specified escalation level
   * @throws IllegalArgumentException if escalationLevel is invalid
   */
  @Transactional(readOnly = true)
  public List<MaintenanceTicket> getTicketsForEscalationLevel(int escalationLevel) {
    if (escalationLevel < 0 || escalationLevel > MaintenanceTicket.getMaxEscalationLevel()) {
      throw new IllegalArgumentException(
          "Invalid escalation level: " + escalationLevel
              + ". Must be between 0 and "
              + MaintenanceTicket.getMaxEscalationLevel());
    }
    return ticketRepository.findByEscalationLevelAndSlaDueAtBefore(
        escalationLevel, LocalDateTime.now());
  }

  /**
   * Update ticket status with state machine validation.
   *
   * <p>Verifies that the transition is valid before updating. Throws IllegalStateException
   * if the transition is not allowed by the state machine.
   *
   * @param ticket the ticket to update (must be non-null and exist in DB)
   * @param newStatus the target status (must be non-null)
   * @return the updated ticket
   * @throws IllegalArgumentException if ticket or newStatus is null
   * @throws IllegalStateException if transition is invalid per state machine
   */
  public MaintenanceTicket updateTicketStatus(MaintenanceTicket ticket, TicketStatus newStatus) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    if (newStatus == null) {
      throw new IllegalArgumentException("Target status cannot be null");
    }

    // Validate transition with state machine
    if (!ticketStateMachine.canTransition(ticket.getStatus(), newStatus)) {
      throw new IllegalStateException(
          "Invalid transition from "
              + ticket.getStatus()
              + " to "
              + newStatus);
    }

    // Perform transition
    ticketStateMachine.transition(ticket, newStatus);
    MaintenanceTicket updatedTicket = ticketRepository.save(ticket);
    log.info(
        "Ticket status updated: ticketId={}, oldStatus={}, newStatus={}",
        ticket.getId(),
        ticket.getStatus(),
        newStatus);

    return updatedTicket;
  }

  /**
   * Assign a ticket to a technician.
   *
   * @param ticket the ticket to assign (must be non-null)
   * @param technician the technician to assign to (can be null to unassign)
   * @return the updated ticket with technician assignment
   * @throws IllegalArgumentException if ticket is null
   */
  public MaintenanceTicket assignTicketToTechnician(
      MaintenanceTicket ticket, User technician) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }

    ticket.setAssignedTechnician(technician);
    MaintenanceTicket updatedTicket = ticketRepository.save(ticket);
    log.info(
        "Ticket assigned: ticketId={}, technician={}",
        ticket.getId(),
        technician != null ? technician.getId() : "UNASSIGNED");

    return updatedTicket;
  }

  /**
   * Increment ticket escalation level (up to maximum of 3).
   *
   * @param ticket the ticket to escalate (must be non-null)
   * @return the updated ticket with escalation level incremented
   * @throws IllegalArgumentException if ticket is null
   * @throws IllegalStateException if escalation level already at maximum
   */
  public MaintenanceTicket escalateTicket(MaintenanceTicket ticket) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }

    int currentLevel = ticket.getEscalationLevel();
    if (currentLevel >= MaintenanceTicket.getMaxEscalationLevel()) {
      throw new IllegalStateException(
          "Ticket escalation already at maximum level: " + currentLevel);
    }

    ticket.setEscalationLevel(currentLevel + 1);
    MaintenanceTicket updatedTicket = ticketRepository.save(ticket);
    log.info(
        "Ticket escalated: ticketId={}, newLevel={}",
        ticket.getId(),
        currentLevel + 1);

    return updatedTicket;
  }

  /**
   * Add a comment to a ticket with specified visibility.
   *
   * <p>All validation is performed at the entity level (NotNull, NotBlank, Size).
   *
   * @param ticket the ticket to comment on (must be non-null)
   * @param author the comment author (must be non-null)
   * @param content the comment text (5-2000 chars)
   * @param visibility the visibility level (PUBLIC or INTERNAL, default PUBLIC)
   * @return the created TicketComment
   * @throws IllegalArgumentException if ticket, author, or content is null/blank
   */
  public TicketComment addComment(
      MaintenanceTicket ticket,
      User author,
      String content,
      TicketCommentVisibility visibility) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    if (author == null) {
      throw new IllegalArgumentException("Author cannot be null");
    }
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("Content cannot be null or blank");
    }

    TicketCommentVisibility effectiveVisibility =
        visibility != null ? visibility : TicketCommentVisibility.PUBLIC;

    TicketComment comment =
        TicketComment.builder()
            .ticket(ticket)
            .author(author)
            .content(content)
            .visibility(effectiveVisibility)
            .build();

    TicketComment savedComment = commentRepository.save(comment);
    log.debug(
        "Comment added: commentId={}, ticket={}, visibility={}",
        savedComment.getId(),
        ticket.getId(),
        effectiveVisibility);

    return savedComment;
  }

  /**
   * Get all visible comments for a ticket based on the viewing user's role.
   *
   * <p><b>Visibility Rules</b>:
   * <ul>
   *   <li><b>Ticket creator</b>: Can see PUBLIC comments only
   *   <li><b>Assigned technician</b>: Can see PUBLIC comments only
   *   <li><b>Staff (TECHNICIAN, FACILITY_MANAGER, ADMIN)</b>: Can see all comments
   * </ul>
   *
   * <p>Soft-deleted comments (deletedAt != null) are always excluded.
   *
   * @param ticket the ticket to get comments for (must be non-null)
   * @param viewingUser the user making the request (can be null for unauthenticated access)
   * @return list of visible comments ordered by createdAt ascending
   * @throws IllegalArgumentException if ticket is null
   */
  @Transactional(readOnly = true)
  public List<TicketComment> getVisibleComments(MaintenanceTicket ticket, User viewingUser) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }

    // If no viewer provided, return empty list (no public comments visible to unauthenticated)
    if (viewingUser == null) {
      return new ArrayList<>();
    }

    // Check if viewer is staff (has any of: TECHNICIAN, FACILITY_MANAGER, ADMIN)
    boolean isStaff =
        viewingUser.getRoles().stream()
            .anyMatch(
                role ->
                    role == Role.TECHNICIAN
                        || role == Role.FACILITY_MANAGER
                        || role == Role.ADMIN);

    // Staff can see all comments
    if (isStaff) {
      return commentRepository.findByTicketAndDeletedAtIsNullOrderByCreatedAtAsc(ticket);
    }

    // Non-staff users can only see PUBLIC comments
    // AND they must be the ticket creator or assigned technician
    boolean isCreatorOrAssigned =
        viewingUser.getId().equals(ticket.getCreatedBy().getId())
            || (ticket.getAssignedTechnician() != null
                && viewingUser.getId().equals(ticket.getAssignedTechnician().getId()));

    if (!isCreatorOrAssigned) {
      return new ArrayList<>();
    }

    return commentRepository.findByTicketAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
        ticket, TicketCommentVisibility.PUBLIC);
  }

  /**
   * Update the content of an existing comment.
   *
   * <p>Only the comment author or ADMIN can update a comment. Validation of content length
   * is delegated to entity bean validation.
   *
   * @param comment the comment to update (must be non-null and not deleted)
   * @param newContent the new comment text (5-2000 chars)
   * @param updateBy the user performing the update (must be non-null)
   * @return the updated comment
   * @throws IllegalArgumentException if comment, newContent, or updateBy is null/blank
   * @throws IllegalStateException if comment is soft-deleted or user lacks permission
   */
  public TicketComment updateComment(
      TicketComment comment, String newContent, User updateBy) {
    if (comment == null) {
      throw new IllegalArgumentException("Comment cannot be null");
    }
    if (newContent == null || newContent.isBlank()) {
      throw new IllegalArgumentException("New content cannot be null or blank");
    }
    if (updateBy == null) {
      throw new IllegalArgumentException("Updated by user cannot be null");
    }

    // Check if already deleted
    if (comment.isDeleted()) {
      throw new IllegalStateException("Cannot update a deleted comment");
    }

    // Check permissions: author or admin
    boolean isAuthor = comment.getAuthor().getId().equals(updateBy.getId());
    boolean isAdmin =
        updateBy.getRoles().stream().anyMatch(role -> role == Role.ADMIN);

    if (!isAuthor && !isAdmin) {
      throw new IllegalStateException("Only comment author or admin can update comment");
    }

    comment.setContent(newContent);
    TicketComment updatedComment = commentRepository.save(comment);
    log.debug(
        "Comment updated: commentId={}, updatedBy={}",
        comment.getId(),
        updateBy.getId());

    return updatedComment;
  }

  /**
   * Soft-delete a comment by setting its deletedAt timestamp.
   *
   * <p>Only the comment author or ADMIN can delete a comment.
   *
   * @param comment the comment to delete (must be non-null and not already deleted)
   * @param deleteBy the user performing the deletion (must be non-null)
   * @return the soft-deleted comment with deletedAt set
   * @throws IllegalArgumentException if comment or deleteBy is null
   * @throws IllegalStateException if comment already deleted or user lacks permission
   */
  public TicketComment deleteComment(TicketComment comment, User deleteBy) {
    if (comment == null) {
      throw new IllegalArgumentException("Comment cannot be null");
    }
    if (deleteBy == null) {
      throw new IllegalArgumentException("Deleted by user cannot be null");
    }

    // Check if already deleted
    if (comment.isDeleted()) {
      throw new IllegalStateException("Comment is already deleted");
    }

    // Check permissions: author or admin
    boolean isAuthor = comment.getAuthor().getId().equals(deleteBy.getId());
    boolean isAdmin =
        deleteBy.getRoles().stream().anyMatch(role -> role == Role.ADMIN);

    if (!isAuthor && !isAdmin) {
      throw new IllegalStateException("Only comment author or admin can delete comment");
    }

    comment.delete(); // Sets deletedAt to now
    TicketComment deletedComment = commentRepository.save(comment);
    log.debug(
        "Comment soft-deleted: commentId={}, deletedBy={}",
        comment.getId(),
        deleteBy.getId());

    return deletedComment;
  }

  /**
   * Get all comments authored by a specific user on a ticket (excluding soft-deleted).
   *
   * @param ticket the ticket to search (must be non-null)
   * @param userId the author user ID (must be non-null)
   * @return list of comments authored by the user on this ticket
   * @throws IllegalArgumentException if ticket or userId is null
   */
  @Transactional(readOnly = true)
  public List<TicketComment> getCommentsByAuthor(MaintenanceTicket ticket, UUID userId) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    return commentRepository.findByTicketAndAuthorIdAndDeletedAtIsNull(ticket, userId);
  }

  /**
   * Count active (non-deleted) comments on a ticket.
   *
   * @param ticket the ticket to count comments for (must be non-null)
   * @return count of active comments
   * @throws IllegalArgumentException if ticket is null
   */
  @Transactional(readOnly = true)
  public long countActiveComments(MaintenanceTicket ticket) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    return commentRepository.countByTicketAndDeletedAtIsNull(ticket);
  }
}
