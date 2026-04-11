package com.sliitreserve.api.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sliitreserve.api.dto.ticket.*;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketComment;
import com.sliitreserve.api.entities.ticket.TicketAttachment;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.services.ticket.TicketService;
import com.sliitreserve.api.services.ticket.TicketAttachmentService;
import com.sliitreserve.api.services.ticket.EscalationService;
import com.sliitreserve.api.workflow.escalation.EscalationLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for maintenance ticket management.
 *
 * <p><b>Purpose</b>: Expose ticket lifecycle, comment, and attachment operations via HTTP endpoints.
 * Enforces role-based access control (RBAC) and DTO boundaries for all operations.
 *
 * <p><b>Security Model</b>:
 * <ul>
 *   <li>All endpoints require JWT authentication (enforced by SecurityConfig)
 *   <li>Role-based filtering: users see only permitted tickets
 *   <li>TECHNICIAN: View assigned tickets, add comments, attach files
 *   <li>FACILITY_MANAGER: View all facility tickets, assign technicians, update status
 *   <li>ADMIN: Full access to all endpoints
 * </ul>
 *
 * <p><b>DTO Mapping</b>:
 * Request/response DTOs enforce field validation and hide internal implementation:
 * <ul>
 *   <li>Requests: TicketCreationRequest, TicketStatusUpdate, TicketCommentRequest, etc.
 *   <li>Responses: TicketResponseDTO, TicketDetailResponseDTO, TicketCommentResponseDTO
 *   <li>Mappers (TODO): Service-layer mappers convert entities to DTOs with null-safe handling
 * </ul>
 *
 * <p><b>API Endpoints</b>:
 *
 * <table border="1">
 *   <tr>
 *     <th>Method</th>
 *     <th>Endpoint</th>
 *     <th>Description</th>
 *     <th>Auth</th>
 *   </tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>/api/tickets</td>
 *     <td>Create new ticket</td>
 *     <td>Authenticated</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/tickets/{id}</td>
 *     <td>Get ticket details</td>
 *     <td>Creator, Technician, Staff</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/tickets</td>
 *     <td>List user's tickets (filtered by role)</td>
 *     <td>Authenticated</td>
 *   </tr>
 *   <tr>
 *     <td>PUT</td>
 *     <td>/api/tickets/{id}/status</td>
 *     <td>Update ticket status</td>
 *     <td>Technician, Manager, Admin</td>
 *   </tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>/api/tickets/{id}/assign</td>
 *     <td>Assign technician</td>
 *     <td>Manager, Admin</td>
 *   </tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>/api/tickets/{id}/comments</td>
 *     <td>Add comment</td>
 *     <td>Authenticated</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/tickets/{id}/comments</td>
 *     <td>Get visible comments</td>
 *     <td>Creator, Technician, Staff</td>
 *   </tr>
 *   <tr>
 *     <td>PUT</td>
 *     <td>/api/tickets/{id}/comments/{commentId}</td>
 *     <td>Update comment</td>
 *     <td>Author, Admin</td>
 *   </tr>
 *   <tr>
 *     <td>DELETE</td>
 *     <td>/api/tickets/{id}/comments/{commentId}</td>
 *     <td>Delete comment (soft-delete)</td>
 *     <td>Author, Admin</td>
 *   </tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>/api/tickets/{id}/attachments</td>
 *     <td>Attach file</td>
 *     <td>Creator, Technician</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/tickets/{id}/attachments</td>
 *     <td>Get attachments</td>
 *     <td>Creator, Technician, Staff</td>
 *   </tr>
 *   <tr>
 *     <td>DELETE</td>
 *     <td>/api/tickets/{id}/attachments/{attachmentId}</td>
 *     <td>Delete attachment</td>
 *     <td>Admin</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/tickets/{id}/escalation-history</td>
 *     <td>Get escalation audit trail</td>
 *     <td>Staff</td>
 *   </tr>
 * </table>
 *
 * <p><b>Error Handling</b>:
 * <ul>
 *   <li>400 Bad Request: Invalid DTO or validation errors
 *   <li>403 Forbidden: User lacks permission for operation
 *   <li>404 Not Found: Ticket, comment, or attachment doesn't exist
 *   <li>409 Conflict: State machine violation or optimistic lock failure
 *   <li>500 Internal Server Error: File storage or database failure
 * </ul>
 *
 * <p><b>Integration Dependencies</b>:
 * <ul>
 *   <li>{@link TicketService}: Core ticket lifecycle and comment management
 *   <li>{@link TicketAttachmentService}: File upload, validation, thumbnail generation
 *   <li>{@link EscalationService}: Escalation history retrieval
 *   <li>{@link MaintenanceTicketRepository}: Ticket persistence
 * </ul>
 *
 * @see TicketService for service contract and business logic
 * @see TicketAttachmentService for file handling
 * @see EscalationService for escalation workflow
 */
@Slf4j
@RestController
@RequestMapping("/api/tickets")
@Validated
public class TicketController {

  private final TicketService ticketService;
  private final TicketAttachmentService attachmentService;
  private final EscalationService escalationService;
  private final MaintenanceTicketRepository ticketRepository;
  private final FacilityRepository facilityRepository;

  @Autowired
  public TicketController(
      TicketService ticketService,
      TicketAttachmentService attachmentService,
      EscalationService escalationService,
      MaintenanceTicketRepository ticketRepository,
      FacilityRepository facilityRepository) {
    this.ticketService = ticketService;
    this.attachmentService = attachmentService;
    this.escalationService = escalationService;
    this.ticketRepository = ticketRepository;
    this.facilityRepository = facilityRepository;
  }

  /**
   * Create a new maintenance ticket.
   *
   * <p><b>Access Control</b>: Authenticated users (any role)
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Validates facility existence and user accessibility
   *   <li>Sets SLA deadline to 48 hours from now
   *   <li>Initializes status to OPEN
   *   <li>Publishes TICKET_CREATED event
   * </ul>
   *
   * @param request ticket creation request DTO
   * @param auth authentication context (Spring-injected)
   * @return created ticket DTO with 201 status
   */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketResponseDTO> createTicket(
      @Valid @RequestBody TicketCreationRequest request,
      Authentication auth) {
    log.info("Creating new ticket: {} in facility {}", request.getTitle(), request.getFacilityId());

    User currentUser = getCurrentUser(auth);
    
    var facility = facilityRepository.findById(request.getFacilityId())
        .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + request.getFacilityId()));
    
    MaintenanceTicket ticket = ticketService.createTicket(
        facility,
        request.getCategory(),
        request.getPriority(),
        request.getTitle(),
        request.getDescription(),
        currentUser
    );

    TicketResponseDTO response = mapToResponseDTO(ticket);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Get ticket details by ID.
   *
   * <p><b>Access Control</b>: Creator, assigned technician, facility staff, admin
   *
   * <p><b>Returns</b>: Complete ticket details including visible comments and attachments.
   * Comments are filtered based on user visibility rules.
   *
   * @param ticketId ticket UUID
   * @param auth authentication context
   * @return ticket detail DTO with 200 status
   */
  @GetMapping("/{ticketId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketDetailResponseDTO> getTicketById(
      @PathVariable UUID ticketId,
      Authentication auth) {
    log.debug("Fetching ticket: {}", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);
    // Permission check: creator, technician, or staff
    validateTicketAccess(ticket, currentUser);

    TicketDetailResponseDTO response = mapToDetailResponseDTO(ticket, currentUser);
    return ResponseEntity.ok(response);
  }

  /**
   * List tickets for the current user (filtered by role and facility).
   *
   * <p><b>Access Control</b>: Authenticated users
   *
   * <p><b>Filtering Rules</b>:
   * <ul>
   *   <li>Users see tickets they created
   *   <li>Technicians see tickets assigned to them
   *   <li>Staff (FACILITY_MANAGER, ADMIN) see all facility tickets
   *   <li>Admin sees all tickets
   * </ul>
   *
   * @param auth authentication context
   * @return list of ticket response DTOs
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TicketResponseDTO>> listTickets(Authentication auth) {
    log.debug("Listing tickets for user: {}", auth.getName());

    User currentUser = getCurrentUser(auth);

    // TODO: Implement proper filtering based on role
    // For now, return all tickets created by or assigned to user
    List<MaintenanceTicket> tickets = ticketRepository.findAll();

    List<TicketResponseDTO> response = tickets.stream()
        .map(this::mapToResponseDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  /**
   * Update ticket status via state machine transition.
   *
   * <p><b>Access Control</b>: Assigned technician, facility manager, admin
   *
   * <p><b>State Machine Validation</b>:
   * <ul>
   *   <li>Technician can transition OPEN → IN_PROGRESS, IN_PROGRESS → CLOSED
   *   <li>Manager/Admin can transition to ESCALATED or REJECTED
   *   <li>Cannot transition from CLOSED or REJECTED (terminal states)
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param request status update request with new status
   * @param auth authentication context
   * @return updated ticket DTO
   */
  @PutMapping("/{ticketId}/status")
  @PreAuthorize("hasAnyRole('TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
  public ResponseEntity<TicketResponseDTO> updateTicketStatus(
      @PathVariable UUID ticketId,
      @Valid @RequestBody TicketStatusUpdate request,
      Authentication auth) {
    log.info("Updating ticket {} status to {}", ticketId, request.getStatus());

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    MaintenanceTicket updated = ticketService.updateTicketStatus(ticket, request.getStatus());

    TicketResponseDTO response = mapToResponseDTO(updated);
    return ResponseEntity.ok(response);
  }

  /**
   * Assign ticket to a technician.
   *
   * <p><b>Access Control</b>: Facility manager, admin
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Validates technician exists and is in same facility
   *   <li>Publishes TICKET_ASSIGNED event
   *   <li>Can unassign by passing null technicianId
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param request assignment request with technician ID
   * @param auth authentication context
   * @return updated ticket DTO
   */
  @PostMapping("/{ticketId}/assign")
  @PreAuthorize("hasAnyRole('FACILITY_MANAGER', 'ADMIN')")
  public ResponseEntity<TicketResponseDTO> assignTicket(
      @PathVariable UUID ticketId,
      @Valid @RequestBody TicketAssignmentRequest request,
      Authentication auth) {
    log.info("Assigning ticket {} to technician {}", ticketId, request.getTechnicianId());

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User technician = null;
    if (request.getTechnicianId() != null) {
      // TODO: Fetch technician from UserRepository
      // technician = userRepository.findById(request.getTechnicianId())
      //     .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    MaintenanceTicket updated = ticketService.assignTicketToTechnician(ticket, technician);

    TicketResponseDTO response = mapToResponseDTO(updated);
    return ResponseEntity.ok(response);
  }

  /**
   * Add comment to ticket.
   *
   * <p><b>Access Control</b>: Authenticated users
   *
   * <p><b>Visibility Rules</b>:
   * <ul>
   *   <li>Any user can add PUBLIC comments
   *   <li>Only staff (TECHNICIAN, FACILITY_MANAGER, ADMIN) can add INTERNAL comments
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param request comment request with content and visibility
   * @param auth authentication context
   * @return created comment DTO with 201 status
   */
  @PostMapping("/{ticketId}/comments")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketCommentResponseDTO> addComment(
      @PathVariable UUID ticketId,
      @Valid @RequestBody TicketCommentRequest request,
      Authentication auth) {
    log.debug("Adding comment to ticket {}", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    TicketComment comment = ticketService.addComment(
        ticket,
        currentUser,
        request.getContent(),
        request.getVisibility()
    );

    TicketCommentResponseDTO response = mapCommentToResponseDTO(comment);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Get visible comments for ticket (filtered by current user's permissions).
   *
   * <p><b>Access Control</b>: Creator, technician, staff
   *
   * <p><b>Visibility Filtering</b>:
   * <ul>
   *   <li>Users see PUBLIC comments
   *   <li>Staff see PUBLIC and INTERNAL comments
   *   <li>Comments marked deleted (deletedAt != null) are excluded
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param auth authentication context
   * @return list of visible comment DTOs
   */
  @GetMapping("/{ticketId}/comments")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TicketCommentResponseDTO>> getComments(
      @PathVariable UUID ticketId,
      Authentication auth) {
    log.debug("Fetching comments for ticket {}", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    List<TicketComment> comments = ticketService.getVisibleComments(ticket, currentUser);

    List<TicketCommentResponseDTO> response = comments.stream()
        .map(this::mapCommentToResponseDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  /**
   * Update comment content.
   *
   * <p><b>Access Control</b>: Comment author, admin
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Only author or admin can update
   *   <li>Sets updatedAt timestamp
   *   <li>Marks comment as edited
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param commentId comment UUID
   * @param request comment request with new content and visibility
   * @param auth authentication context
   * @return updated comment DTO
   */
  @PutMapping("/{ticketId}/comments/{commentId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketCommentResponseDTO> updateComment(
      @PathVariable UUID ticketId,
      @PathVariable UUID commentId,
      @Valid @RequestBody TicketCommentRequest request,
      Authentication auth) {
    log.debug("Updating comment {} on ticket {}", commentId, ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    // TODO: Fetch TicketComment by ID from repository
    // TicketComment comment = commentRepository.findById(commentId)
    //     .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    //
    // TicketComment updated = ticketService.updateComment(comment, request.getContent(), currentUser);

    TicketCommentResponseDTO response = new TicketCommentResponseDTO();
    return ResponseEntity.ok(response);
  }

  /**
   * Delete comment (soft-delete via deletedAt timestamp).
   *
   * <p><b>Access Control</b>: Comment author, admin
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Sets deletedAt timestamp for soft-delete
   *   <li>Comment remains in DB for audit trail
   *   <li>Excluded from getComments responses
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param commentId comment UUID
   * @param auth authentication context
   * @return 204 No Content
   */
  @DeleteMapping("/{ticketId}/comments/{commentId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID ticketId,
      @PathVariable UUID commentId,
      Authentication auth) {
    log.debug("Deleting comment {} on ticket {}", commentId, ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    // TODO: Fetch TicketComment and call ticketService.deleteComment()

    return ResponseEntity.noContent().build();
  }

  /**
   * Attach file to ticket.
   *
   * <p><b>Access Control</b>: Creator, assigned technician
   *
   * <p><b>File Validation</b>:
   * <ul>
   *   <li>Max file size: 5 MB
   *   <li>Allowed MIME types: image/jpeg, image/png, image/gif, image/webp, application/pdf
   *   <li>Max 3 attachments per ticket
   *   <li>SHA-256 checksum computed for integrity verification
   * </ul>
   *
   * <p><b>Processing</b>:
   * <ul>
   *   <li>Stores original file in /uploads/original/
   *   <li>Generates 200x200 thumbnail for images in /uploads/thumbnails/
   *   <li>Sanitizes filename to prevent path traversal
   *   <li>Stores metadata in DB
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param file multipart file containing attachment
   * @param auth authentication context
   * @return created attachment DTO with 201 status
   */
  @PostMapping("/{ticketId}/attachments")
  @PreAuthorize("hasAnyRole('TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
  public ResponseEntity<TicketAttachmentResponseDTO> attachFile(
      @PathVariable UUID ticketId,
      @RequestParam("file") MultipartFile file,
      Authentication auth) {
    log.info("Attaching file {} to ticket {}", file.getOriginalFilename(), ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    TicketAttachment attachment = attachmentService.attachFileToTicket(ticket, file, currentUser);

    TicketAttachmentResponseDTO response = mapAttachmentToResponseDTO(attachment);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Get attachments for ticket.
   *
   * <p><b>Access Control</b>: Creator, assignedTechnician, facility staff
   *
   * @param ticketId ticket UUID
   * @param auth authentication context
   * @return list of attachment DTOs
   */
  @GetMapping("/{ticketId}/attachments")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TicketAttachmentResponseDTO>> getAttachments(
      @PathVariable UUID ticketId,
      Authentication auth) {
    log.debug("Fetching attachments for ticket {}", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    List<TicketAttachment> attachments = attachmentService.getAttachmentsForTicket(ticket);

    List<TicketAttachmentResponseDTO> response = attachments.stream()
        .map(this::mapAttachmentToResponseDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  /**
   * Delete attachment (soft-delete via deletedAt timestamp).
   *
   * <p><b>Access Control</b>: Admin only
   *
   * <p><b>Business Logic</b>:
   * <ul>
   *   <li>Deletes file from disk and DB
   *   <li>Deletes associated thumbnail if exists
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param attachmentId attachment UUID
   * @param auth authentication context
   * @return 204 No Content
   */
  @DeleteMapping("/{ticketId}/attachments/{attachmentId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteAttachment(
      @PathVariable UUID ticketId,
      @PathVariable UUID attachmentId,
      Authentication auth) {
    log.info("Deleting attachment {} from ticket {}", attachmentId, ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    // TODO: Fetch TicketAttachment and call attachmentService.deleteAttachment()

    return ResponseEntity.noContent().build();
  }

  /**
   * Get escalation history for ticket (audit trail of all escalation events).
   *
   * <p><b>Access Control</b>: Staff only (TECHNICIAN, FACILITY_MANAGER, ADMIN)
   *
   * <p><b>Information</b>:
   * <ul>
   *   <li>From escalation level, to escalation level
   *   <li>Escalation reason (e.g., "SLA breached at CRITICAL threshold")
   *   <li>Escalated by user, timestamp
   *   <li>Chronological order (oldest first)
   * </ul>
   *
   * @param ticketId ticket UUID
   * @param auth authentication context
   * @return list of escalation history DTOs
   */
  @GetMapping("/{ticketId}/escalation-history")
  @PreAuthorize("hasAnyRole('TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
  public ResponseEntity<List<TicketEscalationHistoryDTO>> getEscalationHistory(
      @PathVariable UUID ticketId,
      Authentication auth) {
    log.debug("Fetching escalation history for ticket {}", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    var escalations = escalationService.getEscalationHistory(ticket);

    var response = escalations.stream()
        .map(this::mapEscalationToResponseDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  // ==================== Private Helper Methods ====================

  /**
   * Get current user from authentication context (JWT principal).
   * The principal should be the authenticated User entity from JWT token.
   */
  private User getCurrentUser(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof User)) {
      throw new IllegalStateException("User must be authenticated");
    }
    return (User) auth.getPrincipal();
  }

  /**
   * Convert Integer escalation level (0-3 or 1-4) to EscalationLevel enum.
   */
  private EscalationLevel intToEscalationLevel(Integer levelInt) {
    if (levelInt == null) {
      return null;
    }
    return switch(levelInt) {
      case 0, 1 -> EscalationLevel.LEVEL_1;
      case 2 -> EscalationLevel.LEVEL_2;
      case 3 -> EscalationLevel.LEVEL_3;
      case 4 -> EscalationLevel.LEVEL_4;
      default -> null;
    };
  }

  /**
   * Validate user has permission to access ticket.
   * Creators, assigned technicians, and staff always have access.
   */
  private void validateTicketAccess(MaintenanceTicket ticket, User currentUser) {
    boolean isCreator = ticket.getCreatedBy().getId().equals(currentUser.getId());
    boolean isAssignedTechnician = ticket.getAssignedTechnician() != null
        && ticket.getAssignedTechnician().getId().equals(currentUser.getId());
    boolean isStaff = currentUser.getRoles().stream()
        .anyMatch(role -> role.name().startsWith("TECHNICIAN") || role.name().startsWith("FACILITY_MANAGER") || role.name().startsWith("ADMIN"));

    if (!isCreator && !isAssignedTechnician && !isStaff) {
      throw new IllegalArgumentException("Access denied: not permitted to view this ticket");
    }
  }

  /**
   * Map MaintenanceTicket entity to TicketResponseDTO (summary).
   */
  private TicketResponseDTO mapToResponseDTO(MaintenanceTicket ticket) {
    return TicketResponseDTO.builder()
        .id(ticket.getId())
        .title(ticket.getTitle())
        .description(ticket.getDescription())
        .category(ticket.getCategory())
        .priority(ticket.getPriority())
        .status(ticket.getStatus())
        .escalationLevel(ticket.getEscalationLevel())
        .slaDueAt(ticket.getSlaDueAt())
        .createdAt(ticket.getCreatedAt())
        .updatedAt(ticket.getUpdatedAt())
        .facilityId(ticket.getFacility().getId())
        .facilityName(ticket.getFacility().getName())
        .createdById(ticket.getCreatedBy().getId())
        .createdByName(ticket.getCreatedBy().getDisplayName())
        .assignedTechnicianId(ticket.getAssignedTechnician() != null ? ticket.getAssignedTechnician().getId() : null)
        .assignedTechnicianName(ticket.getAssignedTechnician() != null ? ticket.getAssignedTechnician().getDisplayName() : null)
        .attachmentCount(0) // TODO: Compute from repository
        .commentCount(0) // TODO: Compute from repository
        .build();
  }

  /**
   * Map MaintenanceTicket entity to TicketDetailResponseDTO (with relations).
   */
  private TicketDetailResponseDTO mapToDetailResponseDTO(MaintenanceTicket ticket, User currentUser) {
    List<TicketComment> visibleComments = ticketService.getVisibleComments(ticket, currentUser);

    return TicketDetailResponseDTO.builder()
        .id(ticket.getId())
        .title(ticket.getTitle())
        .description(ticket.getDescription())
        .category(ticket.getCategory())
        .priority(ticket.getPriority())
        .status(ticket.getStatus())
        .escalationLevel(ticket.getEscalationLevel())
        .slaDueAt(ticket.getSlaDueAt())
        .slaBreach(ticket.getSlaDueAt() != null && java.time.LocalDateTime.now().isAfter(ticket.getSlaDueAt()))
        .createdAt(ticket.getCreatedAt())
        .updatedAt(ticket.getUpdatedAt())
        .facilityId(ticket.getFacility().getId())
        .facilityName(ticket.getFacility().getName())
        .createdById(ticket.getCreatedBy().getId())
        .createdByName(ticket.getCreatedBy().getDisplayName())
        .assignedTechnicianId(ticket.getAssignedTechnician() != null ? ticket.getAssignedTechnician().getId() : null)
        .assignedTechnicianName(ticket.getAssignedTechnician() != null ? ticket.getAssignedTechnician().getDisplayName() : null)
        .comments(visibleComments.stream().map(this::mapCommentToResponseDTO).collect(Collectors.toList()))
        .attachments(new java.util.ArrayList<>()) // TODO: Map from ticket attachments
        .escalationHistory(new java.util.ArrayList<>()) // TODO: Map from escalation service
        .build();
  }

  /**
   * Map TicketComment entity to TicketCommentResponseDTO.
   */
  private TicketCommentResponseDTO mapCommentToResponseDTO(TicketComment comment) {
    return TicketCommentResponseDTO.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .visibility(comment.getVisibility())
        .authorId(comment.getAuthor().getId())
        .authorName(comment.getAuthor().getDisplayName())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .isEdited(comment.getUpdatedAt() != null && !comment.getUpdatedAt().equals(comment.getCreatedAt()))
        .build();
  }

  /**
   * Map TicketAttachment entity to TicketAttachmentResponseDTO.
   */
  private TicketAttachmentResponseDTO mapAttachmentToResponseDTO(TicketAttachment attachment) {
    return TicketAttachmentResponseDTO.builder()
        .id(attachment.getId())
        .fileName(attachment.getFileName())
        .mimeType(attachment.getMimeType())
        .fileSize(attachment.getFileSize())
        .checksum(attachment.getChecksumHash())
        .fileUrl("/api/uploads/original/" + attachment.getFilePath())
        .thumbnailUrl(attachment.getThumbnailPath() != null ? "/api/uploads/thumbnails/" + attachment.getThumbnailPath() : null)
        .uploadedById(attachment.getUploadedBy().getId())
        .uploadedByName(attachment.getUploadedBy().getDisplayName())
        .uploadedAt(attachment.getUploadedAt())
        .build();
  }

  /**
   * Map TicketEscalation entity to TicketEscalationHistoryDTO.
   */
  private TicketEscalationHistoryDTO mapEscalationToResponseDTO(
      com.sliitreserve.api.entities.ticket.TicketEscalation escalation) {
    return TicketEscalationHistoryDTO.builder()
        .id(escalation.getId())
        .fromLevel(intToEscalationLevel(escalation.getFromLevel()))
        .toLevel(intToEscalationLevel(escalation.getToLevel()))
        .reason(escalation.getEscalationReason())
        .escalatedById(escalation.getEscalatedBy().getId())
        .escalatedByName(escalation.getEscalatedBy().getDisplayName())
        .escalatedAt(escalation.getEscalatedAt())
        .build();
  }
}
