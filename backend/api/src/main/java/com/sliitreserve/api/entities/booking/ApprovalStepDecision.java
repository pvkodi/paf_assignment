package com.sliitreserve.api.entities.booking;

/**
 * ApprovalStep decision enumeration.
 *
 * <p>Represents the decision made at an individual approval step in the workflow:
 * <ul>
 *   <li><b>PENDING</b>: Step awaiting decision (approver has not yet reviewed)
 *   <li><b>APPROVED</b>: Step approved; may proceed to next step or mark booking APPROVED
 *   <li><b>REJECTED</b>: Step rejected; booking is immediately rejected (terminal state)
 * </ul>
 *
 * @see ApprovalStep for the approval step entity
 */
public enum ApprovalStepDecision {
  PENDING,
  APPROVED,
  REJECTED
}
