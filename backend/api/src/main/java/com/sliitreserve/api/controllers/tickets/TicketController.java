package com.sliitreserve.api.controllers.tickets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sliitreserve.api.dto.ticket.*;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketComment;
import com.sliitreserve.api.entities.ticket.TicketCommentVisibility;
import com.sliitreserve.api.entities.ticket.TicketAttachment;
import com.sliitreserve.api.entities.ticket.TicketEscalation;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.ticket.TicketService;
import com.sliitreserve.api.services.ticket.TicketAttachmentService;
import com.sliitreserve.api.services.ticket.EscalationService;
import com.sliitreserve.api.workflow.escalation.EscalationLevel;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
  private final UserRepository userRepository;

  @Autowired
  public TicketController(
      TicketService ticketService,
      TicketAttachmentService attachmentService,
      EscalationService escalationService,
      MaintenanceTicketRepository ticketRepository,
      FacilityRepository facilityRepository,
      UserRepository userRepository) {
    this.ticketService = ticketService;
    this.attachmentService = attachmentService;
    this.escalationService = escalationService;
    this.ticketRepository = ticketRepository;
    this.facilityRepository = facilityRepository;
    this.userRepository = userRepository;
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketResponseDTO> createTicket(
      @Valid @RequestBody TicketCreationRequest request,
      Authentication auth) {
    log.info("Creating ticket: title='{}', category='{}', facilityId='{}'", 
        request.getTitle(), request.getCategory(), request.getFacilityId());
    
    User currentUser = getCurrentUser(auth);
    
    UUID facilityId = request.getFacilityIdAsUUID();
    
    var facility = facilityRepository.findById(facilityId)
        .orElseThrow(() -> {
          log.error("Facility not found: {}", facilityId);
          return new IllegalArgumentException("Facility not found: " + facilityId);
        });
    
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

  @GetMapping("/{ticketId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketDetailResponseDTO> getTicketById(
      @PathVariable UUID ticketId,
      Authentication auth) {
    log.debug("Fetching ticket: {}", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);
    validateTicketAccess(ticket, currentUser);

    TicketDetailResponseDTO response = mapToDetailResponseDTO(ticket, currentUser);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TicketResponseDTO>> listTickets(Authentication auth) {
    log.debug("Listing tickets for user: {}", auth.getName());

    User currentUser = getCurrentUser(auth);

    // Filter tickets by access control: creator, assigned technician, or staff role
    List<MaintenanceTicket> allTickets = ticketRepository.findAll();
    boolean isStaff = currentUser.getRoles().stream()
        .anyMatch(role -> role.name().startsWith("TECHNICIAN") || role.name().startsWith("FACILITY_MANAGER") || role.name().startsWith("ADMIN"));
    
    List<MaintenanceTicket> tickets = allTickets.stream()
        .filter(ticket -> 
            isStaff || 
            ticket.getCreatedBy().getId().equals(currentUser.getId()) ||
            (ticket.getAssignedTechnician() != null && ticket.getAssignedTechnician().getId().equals(currentUser.getId()))
        )
        .collect(Collectors.toList());

    List<TicketResponseDTO> response = tickets.stream()
        .map(this::mapToResponseDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @PutMapping("/{ticketId}/status")
  @PreAuthorize("hasAnyRole('TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
  public ResponseEntity<TicketResponseDTO> updateTicketStatus(
      @PathVariable UUID ticketId,
      @Valid @RequestBody TicketStatusUpdate request,
      Authentication auth) {
    log.info("Updating ticket {} status to {}", ticketId, request.getStatus());

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    // Set rejection reason if rejecting
    if (request.getStatus().name().equals("REJECTED") && request.getRejectionReason() != null) {
      ticket.setRejectionReason(request.getRejectionReason());
    }

    MaintenanceTicket updated = ticketService.updateTicketStatus(ticket, request.getStatus());

    TicketResponseDTO response = mapToResponseDTO(updated);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{ticketId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketResponseDTO> updateTicket(
      @PathVariable UUID ticketId,
      @Valid @RequestBody TicketUpdateRequest request,
      Authentication auth) {
    log.info("Updating ticket {} details", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    // Validate: only creator (OPEN status) or ADMIN can edit
    boolean isCreator = ticket.getCreatedBy().getId().equals(currentUser.getId());
    boolean isAdmin = currentUser.getRoles().stream()
        .anyMatch(role -> role.name().equals("ADMIN"));

    if (!isCreator && !isAdmin) {
      throw new IllegalArgumentException("Not authorized to edit this ticket");
    }

    // If not admin, only OPEN tickets can be edited
    if (!isAdmin && !ticket.getStatus().name().equals("OPEN")) {
      throw new IllegalArgumentException("Cannot edit ticket that is not in OPEN status");
    }

    MaintenanceTicket updated = ticketService.updateTicketDetails(ticket, request);

    TicketResponseDTO response = mapToResponseDTO(updated);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{ticketId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteTicket(
      @PathVariable UUID ticketId,
      Authentication auth) {
    log.info("Deleting ticket {}", ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    // Validate: only creator (OPEN status) or ADMIN can delete
    boolean isCreator = ticket.getCreatedBy().getId().equals(currentUser.getId());
    boolean isAdmin = currentUser.getRoles().stream()
        .anyMatch(role -> role.name().equals("ADMIN"));

    if (!isCreator && !isAdmin) {
      throw new IllegalArgumentException("Not authorized to delete this ticket");
    }

    // If not admin, only OPEN tickets can be deleted
    if (!isAdmin && !ticket.getStatus().name().equals("OPEN")) {
      throw new IllegalArgumentException("Cannot delete ticket that is not in OPEN status");
    }

    ticketService.deleteTicket(ticket);
    return ResponseEntity.noContent().build();
  }

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
      technician = userRepository.findById(request.getTechnicianId())
          .orElseThrow(() -> new IllegalArgumentException("Technician not found: " + request.getTechnicianId()));
    }

    MaintenanceTicket updated = ticketService.assignTicketToTechnician(ticket, technician);

    TicketResponseDTO response = mapToResponseDTO(updated);
    return ResponseEntity.ok(response);
  }

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
        request.getVisibility() != null ? request.getVisibility() : TicketCommentVisibility.PUBLIC
    );

    TicketCommentResponseDTO response = mapCommentToResponseDTO(comment);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

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
    
    // Note: Comment repository method needed - for now finding comments via ticket
    TicketComment comment = null;
    for (TicketComment c : ticket.getComments()) {
      if (c.getId().equals(commentId)) {
        comment = c;
        break;
      }
    }
    if (comment == null) {
      throw new IllegalArgumentException("Comment not found: " + commentId);
    }

    TicketComment updated = ticketService.updateComment(comment, request.getContent(), currentUser);
    TicketCommentResponseDTO response = mapCommentToResponseDTO(updated);
    return ResponseEntity.ok(response);
  }

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
    
    // Find comment by ID in ticket comments
    TicketComment comment = null;
    for (TicketComment c : ticket.getComments()) {
      if (c.getId().equals(commentId)) {
        comment = c;
        break;
      }
    }
    if (comment == null) {
      throw new IllegalArgumentException("Comment not found: " + commentId);
    }

    ticketService.deleteComment(comment, currentUser);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{ticketId}/attachments")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TicketAttachmentResponseDTO> attachFile(
      @PathVariable UUID ticketId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "type", required = false) String type,
      Authentication auth) {
    log.info("Attaching file {} to ticket {} with type {}", file.getOriginalFilename(), ticketId, type);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    TicketAttachment attachment = attachmentService.attachFileToTicket(ticket, file, currentUser, type);

    TicketAttachmentResponseDTO response = mapAttachmentToResponseDTO(attachment);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

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

  @DeleteMapping("/{ticketId}/attachments/{attachmentId}")
  @Transactional
  public ResponseEntity<Void> deleteAttachment(
      @PathVariable UUID ticketId,
      @PathVariable UUID attachmentId,
      Authentication auth) {
    log.info("Deleting attachment {} from ticket {}", attachmentId, ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    // Find attachment in ticket
    TicketAttachment attachment = null;
    for (TicketAttachment a : ticket.getAttachments()) {
      if (a.getId().equals(attachmentId)) {
        attachment = a;
        break;
      }
    }
    if (attachment == null) {
      throw new IllegalArgumentException("Attachment not found: " + attachmentId);
    }

    // Check authorization: owner, admin, or assigned technician can delete
    User currentUser = getCurrentUser(auth);
    boolean isOwner = attachment.getUploadedBy() != null && 
        attachment.getUploadedBy().getId().equals(currentUser.getId());
    boolean isAdmin = auth.getAuthorities().stream()
        .anyMatch(auth2 -> auth2.getAuthority().equals("ROLE_ADMIN"));
    
    // Check if user is a technician assigned to this ticket
    boolean isTechnician = auth.getAuthorities().stream()
        .anyMatch(auth2 -> auth2.getAuthority().equals("ROLE_TECHNICIAN"));
    boolean isAssignedTechnician = isTechnician && 
        ticket.getAssignedTechnician() != null && 
        ticket.getAssignedTechnician().getId().equals(currentUser.getId());
    
    if (!isOwner && !isAdmin && !isAssignedTechnician) {
      throw new IllegalArgumentException("Not authorized to delete this attachment");
    }

    // Remove attachment from ticket's attachments collection
    // With orphanRemoval = true, JPA will automatically delete the orphaned attachment
    ticket.getAttachments().remove(attachment);
    ticketRepository.save(ticket);
    
    // Delete files from disk (handled by service)
    attachmentService.deleteAttachment(attachment);
    
    log.info("Successfully deleted attachment {} from ticket {}", attachmentId, ticketId);
    return ResponseEntity.noContent().build();
  }

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

  @PostMapping("/{ticketId}/escalate")
  @PreAuthorize("hasAnyRole('TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
  public ResponseEntity<TicketEscalationHistoryDTO> manuallyEscalateTicket(
      @PathVariable UUID ticketId,
      @RequestBody com.sliitreserve.api.dto.ticket.ManualEscalationRequest request,
      Authentication auth) {
    log.info("Manual escalation request for ticket {} with reason: {}", 
        ticketId, request.getReason());

    if (request.getReason() == null || request.getReason().isBlank()) {
      throw new IllegalArgumentException("Escalation reason is required");
    }

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

    User currentUser = getCurrentUser(auth);

    try {
      TicketEscalation escalation = escalationService.manuallyEscalateTicket(
          ticket,
          request.getReason(),
          currentUser);

      log.info("Ticket {} escalated manually by user {}", ticketId, currentUser.getDisplayName());

      return ResponseEntity.ok(mapEscalationToResponseDTO(escalation));
    } catch (IllegalStateException e) {
      log.warn("Cannot escalate ticket {}: {}", ticketId, e.getMessage());
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  private User getCurrentUser(Authentication auth) {
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("User must be authenticated");
    }
    
    String email = (String) auth.getPrincipal();
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + email));
  }

  private EscalationLevel intToEscalationLevel(Integer levelInt) {
    if (levelInt == null) {
      return null;
    }
    return switch(levelInt) {
      case 0 -> EscalationLevel.LEVEL_1;
      case 1 -> EscalationLevel.LEVEL_2;
      case 2 -> EscalationLevel.LEVEL_3;
      case 3 -> EscalationLevel.LEVEL_4;
      default -> null;
    };
  }

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

  private TicketResponseDTO mapToResponseDTO(MaintenanceTicket ticket) {
    return TicketResponseDTO.builder()
        .id(ticket.getId())
        .title(ticket.getTitle())
        .description(ticket.getDescription())
        .category(ticket.getCategory())
        .priority(ticket.getPriority())
        .status(ticket.getStatus())
        .rejectionReason(ticket.getRejectionReason())
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
        .attachmentCount(0)
        .commentCount(0)
        .build();
  }

  private TicketDetailResponseDTO mapToDetailResponseDTO(MaintenanceTicket ticket, User currentUser) {
    List<TicketComment> visibleComments = ticketService.getVisibleComments(ticket, currentUser);
    List<TicketAttachment> attachments = attachmentService.getAttachmentsForTicket(ticket);

    return TicketDetailResponseDTO.builder()
        .id(ticket.getId())
        .title(ticket.getTitle())
        .description(ticket.getDescription())
        .category(ticket.getCategory())
        .priority(ticket.getPriority())
        .status(ticket.getStatus())
        .rejectionReason(ticket.getRejectionReason())
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
        .attachments(attachments.stream().map(this::mapAttachmentToResponseDTO).collect(Collectors.toList()))
        .escalationHistory(escalationService.getEscalationHistory(ticket).stream()
            .map(this::mapEscalationToResponseDTO)
            .collect(Collectors.toList()))
        .build();
  }

  private TicketCommentResponseDTO mapCommentToResponseDTO(TicketComment comment) {
    return TicketCommentResponseDTO.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .visibility(comment.getVisibility())
        .authorId(comment.getAuthor().getId())
        .authorName(comment.getAuthor().getDisplayName())
        .authorRoles(new ArrayList<>(comment.getAuthor().getRoles()))
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .isEdited(comment.getUpdatedAt() != null && !comment.getUpdatedAt().equals(comment.getCreatedAt()))
        .build();
  }

  private TicketAttachmentResponseDTO mapAttachmentToResponseDTO(TicketAttachment attachment) {
    return TicketAttachmentResponseDTO.builder()
        .id(attachment.getId())
        .originalFilename(attachment.getFileName())
        .mimeType(attachment.getMimeType())
        .fileSize(attachment.getFileSize())
        .checksumHash(attachment.getChecksumHash())
        .type(attachment.getType())
        .filePath(attachment.getFilePath())
        .thumbnailPath(attachment.getThumbnailPath())
        .uploadedById(attachment.getUploadedBy().getId())
        .uploadedByName(attachment.getUploadedBy().getDisplayName())
        .uploadedAt(attachment.getUploadedAt())
        .build();
  }

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
