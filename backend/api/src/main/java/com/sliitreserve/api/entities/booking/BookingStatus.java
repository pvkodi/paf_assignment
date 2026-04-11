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
 *   <li><b>CHECKED_IN</b>: User successfully checked in within the grace period (15 min);
 *       booking fulfilled.
 *   <li><b>NO_SHOW</b>: User failed to check in within grace period; no-show recorded; counts
 *       toward suspension eligibility (FR-022).
 * </ul>
 */
public enum BookingStatus {
  PENDING,
  APPROVED,
  REJECTED,
  CANCELLED,
  CHECKED_IN,
  NO_SHOW
}
