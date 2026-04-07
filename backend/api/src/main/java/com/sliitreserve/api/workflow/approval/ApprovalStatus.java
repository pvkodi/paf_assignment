package com.sliitreserve.api.workflow.approval;

/**
 * Approval decision status enumeration.
 *
 * <p>Describes the outcome of an approval handler's evaluation:
 * <ul>
 *   <li><b>APPROVED</b>: The approval step has been satisfied. If terminal, the booking becomes
 *       APPROVED; if not terminal, may proceed to the next approval step.
 *   <li><b>REJECTED</b>: The approval step has been denied. The booking is immediately rejected
 *       and the approval chain terminates.
 *   <li><b>PENDING</b>: This approval step defers judgment to the next handler in the chain. The
 *       approval chain continues.
 * </ul>
 *
 * @see ApprovalDecision for the decision result record
 */
public enum ApprovalStatus {
  /** Booking approved at this step. */
  APPROVED,

  /** Booking rejected at this step. */
  REJECTED,

  /** Booking deferred to next approval handler. */
  PENDING
}
