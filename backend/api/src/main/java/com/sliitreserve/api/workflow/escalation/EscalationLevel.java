package com.sliitreserve.api.workflow.escalation;

/**
 * Ticket escalation level enumeration.
 *
 * <p>Represents escalation severity thresholds based on SLA deadline breaches (from FR-032):
 * <ul>
 *   <li><b>LEVEL_1 (CRITICAL)</b>: 4-hour SLA breach. Actions: reassign to senior technician,
 *       HIGH-priority notifications.
 *   <li><b>LEVEL_2 (HIGH)</b>: 8-hour SLA breach. Actions: escalate to facility manager,
 *       HIGH-priority notifications.
 *   <li><b>LEVEL_3 (MEDIUM)</b>: 24-hour SLA breach. Actions: escalate to department head,
 *       STANDARD notifications.
 *   <li><b>LEVEL_4 (LOW)</b>: 72-hour SLA breach. Actions: mark for manual review or auto-close.
 *       No escalation actions required.
 * </ul>
 *
 * <p><b>Time Basis</b>: All SLA calculations use 24x7 elapsed time in campus local timezone with
 * DST support (no exemption for nights, weekends, or holidays).
 *
 * <p>Values map directly to {@code EscalationEvent.level} in the data model for ticket audit
 * trail records.
 *
 * @see EscalationHandler for handlers that evaluate and execute escalations
 * @see EscalationResult for escalation outcomes
 */
public enum EscalationLevel {
  /** CRITICAL: 4-hour SLA threshold. */
  LEVEL_1,

  /** HIGH: 8-hour SLA threshold. */
  LEVEL_2,

  /** MEDIUM: 24-hour SLA threshold. */
  LEVEL_3,

  /** LOW: 72-hour SLA threshold (legacy; no escalation actions). */
  LEVEL_4
}
