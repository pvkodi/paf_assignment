package com.sliitreserve.api.services.ticket;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketEscalation;
import com.sliitreserve.api.workflow.escalation.EscalationResult;

import java.util.List;

/**
 * Service interface for ticket SLA escalation management.
 *
 * <p><b>Purpose</b>: Orchestrate the escalation workflow for maintenance tickets based on
 * SLA deadline breaches. Implements the escalation chain pattern and records escalation audit
 * trails (FR-032, FR-033).
 *
 * <p><b>Key Operations</b>:
 * <ul>
 *   <li>Find tickets with breached SLA deadlines
 *   <li>Evaluate and execute escalations through the handler chain
 *   <li>Record escalation events in the audit trail
 *   <li>Query escalation history for tickets
 * </ul>
 *
 * <p><b>Idempotency</b>: Escalation methods are idempotent; repeated escalation calls on the
 * same ticket at the same level will not duplicate notifications or state changes. Detection
 * relies on tracking the current escalation level in MaintenanceTicket.escalationLevel.
 *
 * @see MaintenanceTicket for ticket entity
 * @see TicketEscalation for escalation audit trail
 * @see com.sliitreserve.api.workflow.escalation.EscalationHandler for handler interface
 */
public interface EscalationService {

  /**
   * Evaluate all tickets with breached SLA deadlines and execute escalations.
   *
   * <p>This is typically called by a scheduler job (e.g., hourly) to process all tickets
   * that have exceeded their SLA due time. For each breached ticket, the escalation handler
   * chain is invoked to perform escalation actions and record audit trails.
   *
   * @return the number of tickets escalated in this run
   */
  int escalateBreachedTickets();

  /**
   * Evaluate a specific ticket for SLA breach and execute escalation if required.
   *
   * <p>Returns immediately if the ticket is already at the maximum escalation level or if
   * the SLA has not been breached.
   *
   * @param ticket the ticket to evaluate for escalation (must be non-null)
   * @return the escalation result containing escalation status and actions taken
   * @throws IllegalArgumentException if ticket is null or lacks required SLA fields
   */
  EscalationResult escalateTicket(MaintenanceTicket ticket);

  /**
   * Evaluate a specific ticket for SLA breach at the current system time.
   *
   * <p>Allows specifying a custom current time for testing purposes.
   *
   * @param ticket the ticket to evaluate
   * @param currentTimeMs the current time in milliseconds since epoch (for testing)
   * @return the escalation result
   * @throws IllegalArgumentException if ticket is null
   */
  EscalationResult escalateTicket(MaintenanceTicket ticket, long currentTimeMs);

  /**
   * Retrieve all escalation events for a ticket in chronological order.
   *
   * @param ticket the ticket to query escalation history for
   * @return list of escalation events, empty if none
   * @throws IllegalArgumentException if ticket is null
   */
  List<TicketEscalation> getEscalationHistory(MaintenanceTicket ticket);

  /**
   * Record an escalation event in the audit trail.
   *
   * <p>This is typically called by the handler chain to record the escalation event
   * for audit and reporting purposes.
   *
   * @param ticket the ticket being escalated
   * @param fromLevel the previous escalation level
   * @param toLevel the new escalation level
   * @param escalatedBy the user/system triggering the escalation
   * @param reason a description of the escalation reason (e.g., "4-hour SLA breach")
   * @return the created TicketEscalation record
   */
  TicketEscalation recordEscalation(
      MaintenanceTicket ticket,
      Integer fromLevel,
      Integer toLevel,
      Object escalatedBy,
      String reason);
}
