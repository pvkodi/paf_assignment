package com.sliitreserve.api.entities.ticket;

/**
 * Maintenance ticket status enumeration.
 *
 * <p><b>Ticket Lifecycle</b> (from FR-025):
 * <ul>
 *   <li><b>OPEN</b>: Initial state after ticket creation; unassigned or waiting triage.
 *   <li><b>IN_PROGRESS</b>: Assigned to technician and work has begun.
 *   <li><b>RESOLVED</b>: Work completed; awaiting closure confirmation.
 *   <li><b>CLOSED</b>: Final resolved state; ticket archived.
 *   <li><b>REJECTED</b>: Invalid, duplicate, or duplicate of existing ticket; terminal state.
 * </ul>
 *
 * <p>State transitions are guarded by a State pattern to enforce valid progressions.
 */
public enum TicketStatus {
  OPEN,
  IN_PROGRESS,
  RESOLVED,
  CLOSED,
  REJECTED
}
