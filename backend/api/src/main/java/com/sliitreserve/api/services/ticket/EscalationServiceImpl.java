package com.sliitreserve.api.services.ticket;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketEscalation;
import com.sliitreserve.api.entities.ticket.TicketStatus;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.ticket.TicketEscalationRepository;
import com.sliitreserve.api.workflow.escalation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
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
  private final CriticalEscalationHandler criticalHandler;
  private final HighEscalationHandler highHandler;
  private final MediumEscalationHandler mediumHandler;
  private final LowEscalationHandler lowHandler;

  @Autowired
  public EscalationServiceImpl(
      MaintenanceTicketRepository ticketRepository,
      TicketEscalationRepository escalationRepository,
      CriticalEscalationHandler criticalHandler,
      HighEscalationHandler highHandler,
      MediumEscalationHandler mediumHandler,
      LowEscalationHandler lowHandler) {
    this.ticketRepository = ticketRepository;
    this.escalationRepository = escalationRepository;
    this.criticalHandler = criticalHandler;
    this.highHandler = highHandler;
    this.mediumHandler = mediumHandler;
    this.lowHandler = lowHandler;
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
      escalatedByUser = (User) escalatedBy;
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

  /**
   * Get or create a system user for recording escalations triggered by the scheduler.
   *
   * <p>In a real system, this would retrieve a pre-configured system admin user.
   * For now, we return a placeholder that represents the system.
   *
   * @return a User representing the system
   */
  private User getSystemUser() {
    // TODO: Retrieve actual system user from database or configuration
    // For now, return a minimal user object
    return User.builder()
        .id(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        .email("system@sliitreserve.edu")
        .displayName("System")
        .build();
  }
}
