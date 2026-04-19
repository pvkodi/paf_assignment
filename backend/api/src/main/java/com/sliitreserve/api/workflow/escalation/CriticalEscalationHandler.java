package com.sliitreserve.api.workflow.escalation;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * CRITICAL (4-hour SLA) escalation handler.
 *
 * <p><b>Purpose</b>: Handle escalation when a ticket's 4-hour SLA deadline is breached.
 *
 * <p><b>Actions Taken</b>:
 * <ul>
 *   <li>Log escalation event
 *   <li>Publish HIGH-severity event to trigger notifications
 *   <li>Record escalation in audit trail
 * </ul>
 *
 * <p><b>SLA Threshold</b>: 4 hours from ticket creation
 *
 * @see AbstractEscalationHandler for chain management
 * @see EscalationLevel#LEVEL_1 for escalation level
 */
@Slf4j
@Component
public class CriticalEscalationHandler extends AbstractEscalationHandler {

  private final EventPublisher eventPublisher;

  public CriticalEscalationHandler(EventPublisher eventPublisher) {
    super(EscalationLevel.LEVEL_1, TimeUnit.HOURS.toMillis(4));
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected EscalationResult executeEscalation(MaintenanceTicket ticket, long currentTimeMs) {
    log.info(
        "Escalating ticket {} to CRITICAL (LEVEL_1): 4-hour SLA breached",
        ticket.getId());

    EscalationResult result =
        EscalationResult.builder()
            .escalated(true)
            .escalationLevel(EscalationLevel.LEVEL_1)
            .message("CRITICAL (4-hour) SLA threshold breached")
            .build()
            .addAction("SLA breached at CRITICAL (4-hour) threshold")
            .addAction("Published HIGH-severity notification event");

    // Publish HIGH-severity event for notifications
    try {
      EventEnvelope event =
          EventEnvelope.builder()
              .eventType("TICKET_ESCALATION_LEVEL_1")
              .severity(EventSeverity.HIGH)
              .entityReference("ticket:" + ticket.getId())
              .title("Ticket Escalated to CRITICAL")
              .description(
                  "Ticket escalated to CRITICAL: "
                      + ticket.getTitle()
                      + " (Priority: "
                      + ticket.getPriority()
                      + ")")
              .source("EscalationService")
              .occurrenceTime(java.time.ZonedDateTime.now())
              .build();

      eventPublisher.publish(event);
      log.debug(
          "Published escalation event for ticket {} at LEVEL_1",
          ticket.getId());
    } catch (Exception e) {
      log.error(
          "Failed to publish escalation event for ticket {}: {}",
          ticket.getId(),
          e.getMessage(),
          e);
      // Don't fail escalation if notification fails
    }

    return result;
  }
}
