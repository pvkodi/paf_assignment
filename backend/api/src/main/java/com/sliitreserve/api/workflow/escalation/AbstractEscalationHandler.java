package com.sliitreserve.api.workflow.escalation;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for escalation handlers implementing the Chain of Responsibility pattern.
 *
 * <p><b>Purpose</b>: Provide common SLA breach evaluation and chain management logic for all
 * escalation handlers. Subclasses implement the actual escalation actions (reassignment,
 * notifications) specific to each escalation level.
 *
 * <p><b>Design Pattern</b>: Template Method + Chain of Responsibility. The {@code handle}
 * method evaluates SLA breach and delegates to abstract {@code executeEscalation} for
 * subclass-specific actions. Supports fluent chaining via {@code setNext}.
 *
 * <p><b>Key Methods</b>:
 * <ul>
 *   <li>{@code isBreach}: Evaluates whether SLA has been breached at this level
 *   <li>{@code executeEscalation}: Abstract method for subclass-specific escalation actions
 *   <li>{@code handle}: Template method orchestrating breach detection and escalation
 * </ul>
 *
 * @see EscalationLevel for escalation level enumeration
 * @see EscalationResult for escalation outcome details
 */
@Slf4j
public abstract class AbstractEscalationHandler implements EscalationHandler {

  private EscalationHandler nextHandler;
  private final EscalationLevel escalationLevel;
  private final long slaThresholdMs;

  /**
   * Initialize the escalation handler with escalation level and SLA threshold.
   *
   * @param escalationLevel the escalation level (LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4)
   * @param slaThresholdMs the SLA threshold in milliseconds from ticket creation
   */
  protected AbstractEscalationHandler(EscalationLevel escalationLevel, long slaThresholdMs) {
    this.escalationLevel = escalationLevel;
    this.slaThresholdMs = slaThresholdMs;
  }

  @Override
  public EscalationResult handle(MaintenanceTicket ticket, long currentTimeMs) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    if (ticket.getSlaDueAt() == null) {
      throw new IllegalArgumentException("Ticket SLA due date is required");
    }

    // Check if SLA has been breached at this level
    if (isBreach(ticket, currentTimeMs)) {
      log.debug(
          "SLA breach detected at {} for ticket {}: threshold={}ms",
          escalationLevel,
          ticket.getId(),
          slaThresholdMs);

      // Execute escalation actions specific to this handler
      EscalationResult result = executeEscalation(ticket, currentTimeMs);

      // Delegate to next handler in chain if present
      if (nextHandler != null) {
        EscalationResult nextResult = nextHandler.handle(ticket, currentTimeMs);
        // Merge results from next handler
        if (!nextResult.getActionsTaken().isEmpty()) {
          result.getActionsTaken().addAll(nextResult.getActionsTaken());
        }
      }

      return result;
    }

    // No breach at this level; return not-escalated result
    EscalationResult result =
        EscalationResult.builder()
            .escalated(false)
            .escalationLevel(escalationLevel)
            .message("SLA not breached at " + escalationLevel)
            .build();

    return result;
  }

  @Override
  public boolean isBreach(MaintenanceTicket ticket, long currentTimeMs) {
    if (ticket == null || ticket.getSlaDueAt() == null) {
      return false;
    }

    // Convert ticket's SLA due time to milliseconds
    long slaDueMs =
        ticket
            .getSlaDueAt()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    // Check if current time exceeds SLA due time + threshold
    long slaBreachTimeMs = slaDueMs + slaThresholdMs;

    return currentTimeMs >= slaBreachTimeMs;
  }

  @Override
  public EscalationHandler setNext(EscalationHandler nextHandler) {
    this.nextHandler = nextHandler;
    return this;
  }

  @Override
  public EscalationHandler getNext() {
    return nextHandler;
  }

  @Override
  public EscalationLevel getEscalationLevel() {
    return escalationLevel;
  }

  @Override
  public long getSlaThresholdMs() {
    return slaThresholdMs;
  }

  /**
   * Execute escalation actions specific to this handler.
   *
   * <p>Subclasses override this to perform escalation operations:
   * <ul>
   *   <li>LEVEL_1: Reassign to senior technician, send HIGH-priority notifications
   *   <li>LEVEL_2: Escalate to facility manager, send HIGH-priority notifications
   *   <li>LEVEL_3: Escalate to department head, send STANDARD notifications
   *   <li>LEVEL_4: Mark for manual review or auto-close
   * </ul>
   *
   * @param ticket the maintenance ticket being escalated
   * @param currentTimeMs the current system time in milliseconds
   * @return an EscalationResult with escalated=true and actions taken
   */
  protected abstract EscalationResult executeEscalation(
      MaintenanceTicket ticket, long currentTimeMs);

  /**
   * Helper method to convert milliseconds to hours for logging and messages.
   *
   * @param ms time in milliseconds
   * @return time in hours (rounded)
   */
  protected double msToHours(long ms) {
    return TimeUnit.MILLISECONDS.toHours(ms);
  }

  /**
   * Helper method to convert LocalDateTime to milliseconds since epoch.
   *
   * @param dateTime the LocalDateTime to convert
   * @return time in milliseconds since epoch
   */
  protected long toEpochMs(LocalDateTime dateTime) {
    if (dateTime == null) {
      return 0;
    }
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  /**
   * Helper method to convert milliseconds since epoch to LocalDateTime.
   *
   * @param epochMs time in milliseconds since epoch
   * @return LocalDateTime in system timezone
   */
  protected LocalDateTime fromEpochMs(long epochMs) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
  }
}
