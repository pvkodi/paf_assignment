package com.sliitreserve.api.workflow.escalation;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventPublisher;
import com.sliitreserve.api.observer.EventSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * HIGH (8-hour SLA) escalation handler.
 *
 * <p><b>Purpose</b>: Handle escalation when a ticket's 8-hour SLA deadline is breached.
 *
 * <p><b>Actions Taken</b>:
 * <ul>
 *   <li>Log escalation event
 *   <li>Publish HIGH-severity event to trigger notifications
 *   <li>Record escalation in audit trail
 * </ul>
 *
 * <p><b>SLA Threshold</b>: 8 hours from ticket creation
 *
 * @see AbstractEscalationHandler for chain management
 * @see EscalationLevel#LEVEL_2 for escalation level
 */
@Slf4j
@Component
public class HighEscalationHandler extends AbstractEscalationHandler {

  private final EventPublisher eventPublisher;

  public HighEscalationHandler(EventPublisher eventPublisher) {
    super(EscalationLevel.LEVEL_2, TimeUnit.HOURS.toMillis(8));
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected EscalationResult executeEscalation(MaintenanceTicket ticket, long currentTimeMs) {
    log.info(
        "Escalating ticket {} to HIGH (LEVEL_2): 8-hour SLA breached",
        ticket.getId());

    EscalationResult result =
        EscalationResult.builder()
            .escalated(true)
            .escalationLevel(EscalationLevel.LEVEL_2)
            .message("HIGH (8-hour) SLA threshold breached")
            .build()
            .addAction("SLA breached at HIGH (8-hour) threshold")
            .addAction("Published HIGH-severity notification event");

    // Publish HIGH-severity event for notifications
    try {
      EventEnvelope event =
          EventEnvelope.builder()
              .eventType("TICKET_ESCALATION_LEVEL_2")
              .severity(EventSeverity.HIGH)
              .entityReference("ticket:" + ticket.getId())
              .title("Ticket Escalated to HIGH")
              .description(
                  "Ticket escalated to HIGH: "
                      + ticket.getTitle()
                      + " (Priority: "
                      + ticket.getPriority()
                      + ")")
              .source("EscalationService")
              .occurrenceTime(java.time.ZonedDateTime.now())
              .build();

      eventPublisher.publish(event);
      log.debug(
          "Published escalation event for ticket {} at LEVEL_2",
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
