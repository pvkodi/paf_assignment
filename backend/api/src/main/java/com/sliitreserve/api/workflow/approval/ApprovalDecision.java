package com.sliitreserve.api.workflow.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Approval decision result record.
 *
 * <p>Represents the outcome of an approval handler's evaluation of a booking request,
 * encapsulating the decision, approver role, and any decision notes or justification.
 *
 * <p><b>Decision Status</b>:
 * <ul>
 *   <li><b>APPROVED</b>: The booking has been approved at this step and may proceed to the next
 *       handler or be marked APPROVED if terminal.
 *   <li><b>REJECTED</b>: The booking has been rejected at this step; the approval chain halts and
 *       the booking status becomes REJECTED.
 *   <li><b>PENDING</b>: This step defers to the next handler (forward to next step in chain).
 * </ul>
 *
 * <p><b>Role Attribution</b>: The {@code approverRole} identifies which approval step made the
 * decision (e.g., "LECTURER", "FACILITY_MANAGER", "ADMIN") for audit logging and user
 * communication.
 *
 * <p><b>Example</b>:
 * <pre>{@code
 * ApprovalDecision approved = ApprovalDecision.builder()
 *     .status(ApprovalStatus.APPROVED)
 *     .approverRole("LECTURER")
 *     .note("Approved: matches lecturer's expertise area")
 *     .build();
 * }</pre>
 *
 * @see ApprovalHandler for the handler interface that produces this decision
 * @see ApprovalStatus for the decision status values
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDecision {

  /**
   * The decision status: APPROVED, REJECTED, or PENDING (defer to next handler).
   */
  private ApprovalStatus status;

  /**
   * The role/approver that made this decision (e.g., "LECTURER", "FACILITY_MANAGER", "ADMIN").
   * Used for attribution in audit logs and user-facing decision records.
   */
  private String approverRole;

  /**
   * Optional note or justification for the decision (e.g., rejection reason, approval
   * conditions).
   */
  private String note;

  /**
   * Check if the decision is a terminal result (APPROVED or REJECTED, not PENDING).
   *
   * @return true if the decision is APPROVED or REJECTED (chain terminates); false if PENDING
   *     (chain continues)
   */
  public boolean isTerminal() {
    return status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED;
  }

  /**
   * Check if the decision is an approval.
   *
   * @return true if status is APPROVED
   */
  public boolean isApproved() {
    return status == ApprovalStatus.APPROVED;
  }

  /**
   * Check if the decision is a rejection.
   *
   * @return true if status is REJECTED
   */
  public boolean isRejected() {
    return status == ApprovalStatus.REJECTED;
  }

  /**
   * Check if the decision defers to the next handler.
   *
   * @return true if status is PENDING
   */
  public boolean isPending() {
    return status == ApprovalStatus.PENDING;
  }
}
