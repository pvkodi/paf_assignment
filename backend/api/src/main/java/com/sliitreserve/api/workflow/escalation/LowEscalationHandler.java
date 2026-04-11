package com.sliitreserve.api.workflow.escalation;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventPublisher;
import com.sliitreserve.api.observer.EventSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * LOW (72-hour SLA) escalation handler.
 *
 * <p><b>Purpose</b>: Handle final escalation when a ticket's 72-hour SLA deadline is breached.
 * This is the maximum escalation level; no further escalations occur.
 *
 * <p><b>Actions Taken</b>:
 * <ul>
 *   <li>Log escalation event
 *   <li>Publish STANDARD-severity event to trigger notifications
 *   <li>Record escalation in audit trail
 *   <li>May trigger manual review or auto-close logic (handled by caller)
 * </ul>
 *
 * <p><b>SLA Threshold</b>: 72 hours from ticket creation (max escalation level)
 *
 * @see AbstractEscalationHandler for chain management
 * @see EscalationLevel#LEVEL_4 for escalation level
 */
@Slf4j
@Component
public class LowEscalationHandler extends AbstractEscalationHandler {

  private final EventPublisher eventPublisher;

  public LowEscalationHandler(EventPublisher eventPublisher) {
    super(EscalationLevel.LEVEL_4, TimeUnit.HOURS.toMillis(72));
    this.eventPublisher = eventPublisher;
  }

  @Override
  protected EscalationResult executeEscalation(MaintenanceTicket ticket, long currentTimeMs) {
    log.warn(
        "Escalating ticket {} to LOW (LEVEL_4): 72-hour SLA breached (max level)",
        ticket.getId());

    EscalationResult result =
        EscalationResult.builder()
            .escalated(true)
            .escalationLevel(EscalationLevel.LEVEL_4)
            .message("LOW (72-hour) SLA threshold breached - maximum escalation level reached")
            .build()
            .addAction("SLA breached at LOW (72-hour) threshold - MAX ESCALATION LEVEL")
            .addAction("Published STANDARD-severity notification event");

    // Publish STANDARD-severity event for notifications
    try {
      EventEnvelope event =
          EventEnvelope.builder()
              .eventType("TICKET_ESCALATION_LEVEL_4")
              .severity(EventSeverity.STANDARD)
              .entityReference("ticket:" + ticket.getId())
              .title("Ticket Escalated to LOW (Maximum Level)")
              .description(
                  "Ticket reached MAXIMUM escalation (LOW/LEVEL_4): "
                      + ticket.getTitle()
                      + " (Priority: "
                      + ticket.getPriority()
                      + ")")
              .source("EscalationService")
              .occurrenceTime(java.time.ZonedDateTime.now())
              .build();

      eventPublisher.publish(event);
      log.debug(
          "Published escalation event for ticket {} at LEVEL_4 (MAX)",
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
