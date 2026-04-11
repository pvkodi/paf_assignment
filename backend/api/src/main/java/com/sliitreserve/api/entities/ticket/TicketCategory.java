package com.sliitreserve.api.entities.ticket;

/**
 * Maintenance ticket category enumeration.
 *
 * <p>Categorizes tickets for routing, expertise matching, and SLA severity mapping.
 */
public enum TicketCategory {
  ELECTRICAL,
  PLUMBING,
  HVAC,
  IT_NETWORKING,
  STRUCTURAL,
  CLEANING,
  SAFETY,
  OTHER
}
