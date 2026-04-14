package com.sliitreserve.api.controllers.tickets;

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
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.services.ticket.TicketService;
import com.sliitreserve.api.services.ticket.TicketAttachmentService;
import com.sliitreserve.api.services.ticket.EscalationService;
import com.sliitreserve.api.workflow.escalation.EscalationLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    List<MaintenanceTicket> tickets = ticketRepository.findAll();

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

    User currentUser = getCurrentUser(auth);

    MaintenanceTicket updated = ticketService.updateTicketStatus(ticket, request.getStatus());

    TicketResponseDTO response = mapToResponseDTO(updated);
    return ResponseEntity.ok(response);
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
      // TODO: Fetch technician from UserRepository
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
        request.getVisibility()
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

    TicketCommentResponseDTO response = new TicketCommentResponseDTO();
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

    return ResponseEntity.noContent().build();
  }

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
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteAttachment(
      @PathVariable UUID ticketId,
      @PathVariable UUID attachmentId,
      Authentication auth) {
    log.info("Deleting attachment {} from ticket {}", attachmentId, ticketId);

    MaintenanceTicket ticket = ticketRepository.findById(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

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

  private User getCurrentUser(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof User)) {
      throw new IllegalStateException("User must be authenticated");
    }
    return (User) auth.getPrincipal();
  }

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
        .attachments(new java.util.ArrayList<>())
        .escalationHistory(new java.util.ArrayList<>())
        .build();
  }

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
