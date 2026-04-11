package com.sliitreserve.api.workflow.escalation;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;

/**
 * Chain of Responsibility interface for ticket SLA escalation workflows.
 *
 * <p><b>Purpose</b>: Define the contract for SLA escalation level handlers in an extensible
 * escalation chain. Handlers evaluate maintenance tickets for SLA deadline breaches and execute
 * appropriate escalation actions (reassignment, notifications, alerts) at each severity level.
 *
 * <p><b>Design Pattern</b>: Chain of Responsibility. Each handler is responsible for:
 * <ul>
 *   <li>Evaluating whether the ticket's SLA deadline has been breached at this escalation level
 *   <li>Performing escalation actions (reassignment, notifications, state transitions)
 *   <li>Delegating to the next escalation level if the breach persists
 * </ul>
 *
 * <p><b>SLA Thresholds and Escalation Rules</b> (from FR-032, FR-033):
 * <ul>
 *   <li><b>CRITICAL (LEVEL_1)</b>: 4-hour SLA breach → reassign to senior technician, send
 *       HIGH-priority notifications (email + in-app)
 *   <li><b>HIGH (LEVEL_2)</b>: 8-hour SLA breach → escalate to facility manager, HIGH-priority
 *       notifications
 *   <li><b>MEDIUM (LEVEL_3)</b>: 24-hour SLA breach → escalate to department head, STANDARD
 *       notifications
 *   <li><b>LOW (LEVEL_4)</b>: 72-hour SLA → auto-resolve or mark for manual review (no escalation)
 * </ul>
 *
 * <p><b>Time Basis</b>: All SLA calculations use 24x7 elapsed time in campus local timezone with
 * DST support (no exemption for nights, weekends, or holidays).
 *
 * <p><b>Typical Chain Order</b>:
 * <ol>
 *   <li>CriticalEscalationHandler (4h threshold, LEVEL_1)
 *   <li>HighEscalationHandler (8h threshold, LEVEL_2)
 *   <li>MediumEscalationHandler (24h threshold, LEVEL_3)
 *   <li>LowEscalationHandler (72h threshold, terminal) - optional, no escalation
 * </ol>
 *
 * <p><b>Example Usage</b>:
 * <pre>{@code
 * EscalationHandler chain = new CriticalEscalationHandler()
 *     .setNext(new HighEscalationHandler()
 *         .setNext(new MediumEscalationHandler()
 *             .setNext(new LowEscalationHandler())));
 *
 * EscalationResult result = chain.handle(ticket, currentTime);
 * // result contains: escalated (boolean), escalationLevel, actionsTaken, notifications
 * }</pre>
 *
 * <p><b>State Management</b>: Each handler is stateless and reusable; all context (ticket,
 * current time for SLA comparison) is passed through method parameters to enable concurrent
 * invocations. Escalation actions (reassignment, notifications) are idempotent where possible.
 *
 * <p><b>Notification Integration</b>: The handler delegates actual notification dispatch to the
 * {@link com.sliitreserve.api.observers.EventPublisher} for HIGH/STANDARD severity routing (FR-034,
 * FR-035).
 *
 * @see EscalationResult for the escalation result
 * @see EscalationLevel for the escalation level enumeration
 */
public interface EscalationHandler {

  /**
   * Evaluate SLA breach and execute escalation actions if required.
   *
   * <p>Implementations MUST:
   * <ul>
   *   <li>Compare ticket's SLA due time against current time
   *   <li>If SLA is breached at this level, perform escalation actions (reassign, notify) and
   *       return escalated=true
   *   <li>If SLA is not breached, return escalated=false (may continue to next handler)
   *   <li>If SLA is breached, delegate to next handler if registered (for multi-level escalation)
   *   <li>Return the {@link EscalationResult} with escalation details
   *   <li>Be idempotent: repeated escalation calls should not duplicate notifications or
   *       reassignments
   * </ul>
   *
   * @param ticket the maintenance ticket being evaluated for escalation
   * @param currentTimeMs the current system time in milliseconds (for reproducible SLA
   *     comparisons)
   * @return an {@link EscalationResult} with escalated flag, level, actions taken, and
   *     notifications triggered
   * @throws IllegalArgumentException if required context is missing (ticket is null or has no SLA
   *     due time)
   */
  EscalationResult handle(MaintenanceTicket ticket, long currentTimeMs);

  /**
   * Determine whether this escalation level's SLA threshold has been breached.
   *
   * <p>Implementations MUST compare the ticket's SLA due time against the current time (in campus
   * local timezone with DST support) and return true only if the compared time has exceeded the
   * SLA deadline for this level.
   *
   * <p>Example: A CRITICAL (4-hour) handler returns true if current time is >4 hours after
   * ticket creation (or last escalation action).
   *
   * @param ticket the ticket to evaluate
   * @param currentTimeMs the current system time in milliseconds
   * @return true if SLA has been breached at this escalation level; false otherwise
   */
  boolean isBreach(MaintenanceTicket ticket, long currentTimeMs);

  /**
   * Register the next handler in the escalation chain.
   *
   * <p>This method establishes the chain link; handlers invoke {@link
   * #handle(MaintenanceTicket, long)} on the next handler when escalation actions are executed
   * (to evaluate the next escalation level).
   *
   * @param nextHandler the next escalation handler in the chain
   * @return this handler for fluent chaining: {@code handler.setNext(next1).setNext(next2)}
   */
  EscalationHandler setNext(EscalationHandler nextHandler);

  /**
   * Retrieve the next handler in the escalation chain.
   *
   * @return the next handler, or null if this is the terminal handler
   */
  EscalationHandler getNext();

  /**
   * Get the escalation level for this handler.
   *
   * @return the {@link EscalationLevel} of this handler (LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4)
   */
  EscalationLevel getEscalationLevel();

  /**
   * Get the SLA threshold in milliseconds for this escalation level.
   *
   * @return SLA elapsed time threshold (e.g., 4 hours in ms for CRITICAL, 8 hours for HIGH, etc.)
   */
  long getSlaThresholdMs();
}
