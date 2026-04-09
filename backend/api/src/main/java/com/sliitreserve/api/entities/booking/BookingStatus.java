package com.sliitreserve.api.entities.booking;

/**
 * Booking status enumeration.
 *
 * <p><b>Booking Lifecycle</b>:
 * <ul>
 *   <li><b>PENDING</b>: Initial state after booking creation; awaiting approvals.
 *   <li><b>APPROVED</b>: All required approvals obtained; booking is confirmed.
 *   <li><b>REJECTED</b>: Approval chain rejected the booking; terminal state.
 *   <li><b>CANCELLED</b>: User or admin cancelled the booking after approval but before start;
 *       terminal state.
 * </ul>
 */
public enum BookingStatus {
  PENDING,
  APPROVED,
  REJECTED,
  CANCELLED
}
