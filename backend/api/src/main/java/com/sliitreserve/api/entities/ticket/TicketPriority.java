package com.sliitreserve.api.entities.ticket;

/**
 * Maintenance ticket priority enumeration.
 *
 * <p>Determines SLA deadlines and escalation urgency (from FR-032):
 * <ul>
 *   <li><b>CRITICAL</b>: 4-hour SLA (LEVEL_1 escalation)
 *   <li><b>HIGH</b>: 8-hour SLA (LEVEL_2 escalation)
 *   <li><b>MEDIUM</b>: 24-hour SLA (LEVEL_3 escalation)
 *   <li><b>LOW</b>: 72-hour SLA (LEVEL_4, no escalation)
 * </ul>
 *
 * <p>SLA clocks are 24x7 elapsed time in campus local timezone (no exemption for nights,
 * weekends, or holidays).
 */
public enum TicketPriority {
  LOW,
  MEDIUM,
  HIGH,
  CRITICAL
}
