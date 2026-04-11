package com.sliitreserve.api.entities.ticket;

/**
 * Ticket comment visibility enumeration.
 *
 * <p>Controls who can view a comment on a maintenance ticket.
 * <ul>
 *   <li><b>PUBLIC</b>: Visible to both users and staff
 *   <li><b>INTERNAL</b>: Visible to staff only (TECHNICIAN, FACILITY_MANAGER, ADMIN)
 * </ul>
 */
public enum TicketCommentVisibility {
  PUBLIC,
  INTERNAL
}
