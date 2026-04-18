package com.sliitreserve.api.services.ticket;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketEscalation;
import com.sliitreserve.api.entities.ticket.TicketStatus;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.ticket.TicketEscalationRepository;
import com.sliitreserve.api.workflow.escalation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of EscalationService for ticket SLA escalation management.
 *
 * <p><b>Purpose</b>: Orchestrate the escalation workflow for maintenance tickets based on
 * SLA deadline breaches. Builds and executes the escalation handler chain and records audit
 * trails for all escalation events (FR-032, FR-033).
 *
 * <p><b>Escalation Chain</b>:
 * <ul>
 *   <li>CRITICAL (LEVEL_1): 4-hour SLA breach
 *   <li>HIGH (LEVEL_2): 8-hour SLA breach
 *   <li>MEDIUM (LEVEL_3): 24-hour SLA breach
 *   <li>LOW (LEVEL_4): 72-hour SLA breach (max level)
 * </ul>
 *
 * <p><b>Idempotency</b>: Escalations are idempotent by tracking the current escalation level
 * in the MaintenanceTicket.escalationLevel field. Repeated escalations at the same level are
 * no-ops.
 *
 * <p><b>Typical Usage</b> (from SlaScheduler):
 * <pre>{@code
 * // Called hourly by scheduler to process all breached tickets
 * int escalatedCount = escalationService.escalateBreachedTickets();
 * log.info("Escalated {} tickets", escalatedCount);
 * }</pre>
 *
 * @see MaintenanceTicket for ticket entity
 * @see TicketEscalation for escalation audit trail
 * @see EscalationHandler for the handler interface
 */
@Slf4j
@Service
@Transactional
public class EscalationServiceImpl implements EscalationService {

  private final MaintenanceTicketRepository ticketRepository;
  private final TicketEscalationRepository escalationRepository;
  private final UserRepository userRepository;
  private final CriticalEscalationHandler criticalHandler;
  private final HighEscalationHandler highHandler;
  private final MediumEscalationHandler mediumHandler;
  private final LowEscalationHandler lowHandler;
  private final EventPublisher eventPublisher;

  @Autowired
  public EscalationServiceImpl(
      MaintenanceTicketRepository ticketRepository,
      TicketEscalationRepository escalationRepository,
      UserRepository userRepository,
      CriticalEscalationHandler criticalHandler,
      HighEscalationHandler highHandler,
      MediumEscalationHandler mediumHandler,
      LowEscalationHandler lowHandler,
      EventPublisher eventPublisher) {
    this.ticketRepository = ticketRepository;
    this.escalationRepository = escalationRepository;
    this.userRepository = userRepository;
    this.criticalHandler = criticalHandler;
    this.highHandler = highHandler;
    this.mediumHandler = mediumHandler;
    this.lowHandler = lowHandler;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public int escalateBreachedTickets() {
    log.info("Starting escalation of breached tickets");

    // Find all non-terminal tickets with breached SLA
    List<MaintenanceTicket> breachedTickets = ticketRepository.findByStatusNotInAndSlaDueAtBefore(
        Arrays.asList(TicketStatus.CLOSED, TicketStatus.REJECTED), LocalDateTime.now());

    if (breachedTickets.isEmpty()) {
      log.debug("No breached tickets found for escalation");
      return 0;
    }

    int escalatedCount = 0;
    long currentTimeMs = System.currentTimeMillis();

    for (MaintenanceTicket ticket : breachedTickets) {
      try {
        EscalationResult result = escalateTicket(ticket, currentTimeMs);
        if (result.wasEscalated()) {
          escalatedCount++;
        }
      } catch (Exception e) {
        log.error(
            "Error escalating ticket {}: {}",
            ticket.getId(),
            e.getMessage(),
            e);
        // Continue with next ticket on error
      }
    }

    log.info(
        "Escalation batch complete: {} of {} breached tickets escalated",
        escalatedCount,
        breachedTickets.size());
    return escalatedCount;
  }

  @Override
  public EscalationResult escalateTicket(MaintenanceTicket ticket) {
    return escalateTicket(ticket, System.currentTimeMillis());
  }

  @Override
  public EscalationResult escalateTicket(MaintenanceTicket ticket, long currentTimeMs) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }

    // Skip if already at maximum escalation level
    if (ticket.getEscalationLevel() >= 4) {
      log.debug(
          "Ticket {} already at maximum escalation level",
          ticket.getId());
      return EscalationResult.builder()
          .escalated(false)
          .message("Ticket already at maximum escalation level")
          .build();
    }

    log.debug(
        "Evaluating ticket {} for escalation (current level: {})",
        ticket.getId(),
        ticket.getEscalationLevel());

    // Build the escalation handler chain
    EscalationHandler chain = buildEscalationChain();

    // Evaluate ticket through the chain
    EscalationResult result = chain.handle(ticket, currentTimeMs);

    // If escalated, update ticket and record audit trail
    if (result.wasEscalated()) {
      int oldLevel = ticket.getEscalationLevel();
      int newLevel = oldLevel + 1;

      ticket.setEscalationLevel(newLevel);
      MaintenanceTicket updatedTicket = ticketRepository.save(ticket);

      // Record escalation event in audit trail
      recordEscalation(
          updatedTicket,
          oldLevel,
          newLevel,
          getSystemUser(),
          result.getMessage());

      log.info(
          "Ticket {} escalated from level {} to {}",
          ticket.getId(),
          oldLevel,
          newLevel);

      // Assign to least busy technician and send notification
      try {
        Optional<User> leastBusy = findLeastBusyTechnician(updatedTicket.getFacility().getId());
        if (leastBusy.isPresent()) {
          User assignee = leastBusy.get();
          updatedTicket.setAssignedTechnician(assignee);
          ticketRepository.save(updatedTicket);
          log.info("Ticket {} auto-assigned to {} after escalation", 
              updatedTicket.getId(), assignee.getDisplayName());

          // Send notification to assigned technician
          sendEscalationNotification(updatedTicket, assignee, newLevel, result.getMessage());
        } else {
          log.warn("No technicians available to assign escalated ticket: {}", updatedTicket.getId());
        }
      } catch (Exception e) {
        log.warn("Failed to auto-assign escalated ticket {}: {}", updatedTicket.getId(), e.getMessage());
        // Don't throw - assignment failure shouldn't block escalation
      }
    }

    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TicketEscalation> getEscalationHistory(MaintenanceTicket ticket) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    return escalationRepository.findByTicketOrderByEscalatedAtAsc(ticket);
  }

  @Override
  public TicketEscalation recordEscalation(
      MaintenanceTicket ticket,
      Integer fromLevel,
      Integer toLevel,
      Object escalatedBy,
      String reason) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    if (fromLevel == null || toLevel == null) {
      throw new IllegalArgumentException("From and to levels are required");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Escalation reason is required");
    }

    User escalatedByUser = null;
    if (escalatedBy instanceof User) {
      User user = (User) escalatedBy;
      // Reload user from database to ensure it's managed by Hibernate
      // The user from Spring Security is detached and will cause issues when saving
      escalatedByUser = userRepository.findById(user.getId())
          .orElseThrow(() -> new IllegalArgumentException("Escalating user not found: " + user.getId()));
    } else {
      // If no user provided, use a system user placeholder (would be admin user in real app)
      escalatedByUser = getSystemUser();
    }

    TicketEscalation escalation =
        TicketEscalation.builder()
            .ticket(ticket)
            .fromLevel(fromLevel)
            .toLevel(toLevel)
            .escalatedBy(escalatedByUser)
            .escalationReason(reason)
            .previousAssignee(ticket.getAssignedTechnician())
            .build();

    TicketEscalation savedEscalation = escalationRepository.save(escalation);
    log.debug(
        "Recorded escalation event: ticket={}, {}->{}, reason={}",
        ticket.getId(),
        fromLevel,
        toLevel,
        reason);

    return savedEscalation;
  }

  /**
   * Build the escalation handler chain: CRITICAL -> HIGH -> MEDIUM -> LOW.
   *
   * @return the chain of handlers starting with CRITICAL
   */
  private EscalationHandler buildEscalationChain() {
    return criticalHandler
        .setNext(highHandler
            .setNext(mediumHandler
                .setNext(lowHandler)));
  }

  @Override
  @Transactional
  public TicketEscalation manuallyEscalateTicket(
      MaintenanceTicket ticket,
      String reason,
      Object escalatingUser) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Escalation reason is required");
    }
    if (escalatingUser == null) {
      throw new IllegalArgumentException("Escalating user cannot be null");
    }

    // Check if already at max level
    Integer currentLevel = ticket.getEscalationLevel();
    if (currentLevel != null && currentLevel >= 3) {
      throw new IllegalStateException(
          "Ticket is already at maximum escalation level (LEVEL_4)");
    }

    // Calculate next level
    Integer nextLevel = (currentLevel == null || currentLevel < 0) ? 1 : currentLevel + 1;

    // Ensure nextLevel doesn't exceed max
    if (nextLevel > 3) {
      nextLevel = 3;
    }

    // Update ticket escalation level
    ticket.setEscalationLevel(nextLevel);
    ticketRepository.save(ticket);

    // Record the escalation with the actual user
    User escalatedByUser = null;
    if (escalatingUser instanceof User) {
      escalatedByUser = (User) escalatingUser;
    } else {
      throw new IllegalArgumentException(
          "Escalating user must be a User entity");
    }

    return recordEscalation(
        ticket,
        currentLevel != null ? currentLevel : 0,
        nextLevel,
        escalatedByUser,
        reason);
  }

  @Override
  @Transactional
  public TicketEscalation manuallyEscalateTicket(
      MaintenanceTicket ticket,
      String reason,
      Object escalatingUser,
      UUID assigneeId) {
    // First, escalate the ticket to the next level
    TicketEscalation escalation = manuallyEscalateTicket(ticket, reason, escalatingUser);

    // Then assign it to the specified staff member
    if (assigneeId != null) {
      User assignee = userRepository.findById(assigneeId)
          .orElseThrow(() -> new IllegalArgumentException("Assignee not found: " + assigneeId));
      
      ticket.setAssignedTechnician(assignee);
      ticketRepository.save(ticket);
      
      log.info("Ticket {} assigned to {} after escalation", ticket.getId(), assignee.getDisplayName());

      // Send notification to assigned staff member about escalation
      try {
        String levelDisplay = formatEscalationLevel(ticket.getEscalationLevel());
        eventPublisher.publish(EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("TICKET_ESCALATED_AND_ASSIGNED")
            .severity(EventSeverity.HIGH)
            .affectedUserId(assignee.getId().getMostSignificantBits())
            .title("Ticket Escalated & Assigned to You")
            .description("Ticket #" + ticket.getId() + " has been escalated to " + levelDisplay + " and assigned to you for immediate action.")
            .source("EscalationService")
            .occurrenceTime(ZonedDateTime.now(ZoneId.systemDefault()))
            .entityReference("ticket:" + ticket.getId())
            .actionUrl("/tickets/" + ticket.getId())
            .actionLabel("View Ticket")
            .metadata(Map.of(
                "userId", assignee.getId().toString(),
                "ticketId", ticket.getId().toString(),
                "escalationLevel", ticket.getEscalationLevel().toString(),
                "escalationReason", reason,
                "assignedTo", assignee.getEmail()
            ))
            .build());
        log.info("Escalation notification sent to {}", assignee.getEmail());
      } catch (Exception e) {
        log.warn("Failed to send escalation notification to {}: {}", assignee.getEmail(), e.getMessage());
        // Don't throw - notification failure shouldn't block escalation
      }
    }

    return escalation;
  }

  private String formatEscalationLevel(Integer level) {
    return switch(level) {
      case 1 -> "Level 1";
      case 2 -> "Level 2";
      case 3 -> "Level 3";
      case 4 -> "Level 4 (Critical)";
      default -> "Unknown Level";
    };
  }

  /**
   * Get or create a system user for recording escalations triggered by the scheduler.
   *
   * <p>Uses the system admin user from seed data. This ensures foreign key constraint
   * is satisfied in the ticket_escalation table.
   *
   * @return a User representing the system
   */
  private User getSystemUser() {
    // Use the system admin user from seed data (stable UUID from V2__seed_core_data.sql)
    return User.builder()
        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        .email("admin@smartcampus.edu")
        .displayName("System Admin")
        .build();
  }

  @Override
  public Optional<User> findLeastBusyTechnician(UUID facilityId) {
    // Get all active technicians (all technicians can be assigned tickets in any facility)
    var technicians = userRepository.findByRoleAndActiveTrue(com.sliitreserve.api.entities.auth.Role.TECHNICIAN);
    
    if (technicians.isEmpty()) {
      log.warn("No active technicians available for assignment");
      return Optional.empty();
    }

    // Find technician with fewest active tickets
    User leastBusy = null;
    long minActiveTickets = Long.MAX_VALUE;

    for (User tech : technicians) {
      long activeCount = ticketRepository.countActiveTicketsByTechnician(tech.getId());
      if (activeCount < minActiveTickets) {
        minActiveTickets = activeCount;
        leastBusy = tech;
      }
    }

    if (leastBusy != null) {
      log.info("Selected least busy technician {} with {} active tickets",
          leastBusy.getEmail(), minActiveTickets);
    }

    return Optional.ofNullable(leastBusy);
  }

  private void sendEscalationNotification(
      MaintenanceTicket ticket,
      User assignee,
      Integer escalationLevel,
      String escalationReason) {
    try {
      String levelDisplay = formatEscalationLevel(escalationLevel);
      eventPublisher.publish(EventEnvelope.builder()
          .eventId(UUID.randomUUID().toString())
          .eventType("TICKET_ESCALATED_AND_ASSIGNED")
          .severity(EventSeverity.HIGH)
          .affectedUserId(assignee.getId().getMostSignificantBits())
          .title("Ticket Escalated & Assigned to You")
          .description("Ticket #" + ticket.getId() + " has been escalated to " + levelDisplay + 
              " and automatically assigned to you due to: " + escalationReason)
          .source("EscalationService")
          .occurrenceTime(ZonedDateTime.now(ZoneId.systemDefault()))
          .entityReference("ticket:" + ticket.getId())
          .actionUrl("/tickets/" + ticket.getId())
          .actionLabel("View Ticket")
          .metadata(Map.of(
              "userId", assignee.getId().toString(),
              "ticketId", ticket.getId().toString(),
              "escalationLevel", escalationLevel.toString(),
              "escalationReason", escalationReason,
              "assignedTo", assignee.getEmail()
          ))
          .build());
      log.info("Auto-escalation notification sent to {}", assignee.getEmail());
    } catch (Exception e) {
      log.warn("Failed to send auto-escalation notification to {}: {}", 
          assignee.getEmail(), e.getMessage());
      // Don't throw - notification failure shouldn't block escalation
    }
  }
}
