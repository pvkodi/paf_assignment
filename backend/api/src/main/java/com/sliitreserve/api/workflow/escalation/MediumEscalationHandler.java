package com.sliitreserve.api.workflow.escalation;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventPublisher;
import com.sliitreserve.api.observer.EventSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MEDIUM (24-hour SLA) escalation handler.
 *
 * <p><b>Purpose</b>: Handle escalation when a ticket's 24-hour SLA deadline is breached.
 *
 * <p><b>Actions Taken</b>:
 * <ul>
 *   <li>Log escalation event
 *   <li>Publish STANDARD-severity event to trigger notifications
 *   <li>Record escalation in audit trail
 * </ul>
 *
 * <p><b>SLA Threshold</b>: 24 hours from ticket creation
 *
 * @see AbstractEscalationHandler for chain management
 * @see EscalationLevel#LEVEL_3 for escalation level
 */
@Slf4j
@Component
public class MediumEscalationHandler extends AbstractEscalationHandler {

  private final EventPublisher eventPublisher;

  public MediumEscalationHandler(EventPublisher eventPublisher) {
    super(EscalationLevel.LEVEL_3, TimeUnit.HOURS.toMillis(24));
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected EscalationResult executeEscalation(MaintenanceTicket ticket, long currentTimeMs) {
    log.info(
        "Escalating ticket {} to MEDIUM (LEVEL_3): 24-hour SLA breached",
        ticket.getId());

    EscalationResult result =
        EscalationResult.builder()
            .escalated(true)
            .escalationLevel(EscalationLevel.LEVEL_3)
            .message("MEDIUM (24-hour) SLA threshold breached")
            .build()
            .addAction("SLA breached at MEDIUM (24-hour) threshold")
            .addAction("Published STANDARD-severity notification event");

    // Publish STANDARD-severity event for notifications
    try {
      EventEnvelope event =
          EventEnvelope.builder()
              .eventType("TICKET_ESCALATION_LEVEL_3")
              .severity(EventSeverity.STANDARD)
              .entityReference("ticket:" + ticket.getId())
              .title("Ticket Escalated to MEDIUM")
              .description(
                  "Ticket escalated to MEDIUM: "
                      + ticket.getTitle()
                      + " (Priority: "
                      + ticket.getPriority()
                      + ")")
              .source("EscalationService")
              .occurrenceTime(java.time.ZonedDateTime.now())
              .build();

      eventPublisher.publish(event);
      log.debug(
          "Published escalation event for ticket {} at LEVEL_3",
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
