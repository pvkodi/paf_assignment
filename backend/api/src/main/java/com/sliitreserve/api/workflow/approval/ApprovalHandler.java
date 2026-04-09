package com.sliitreserve.api.workflow.approval;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.User;

/**
 * Chain of Responsibility interface for booking approval workflows.
 *
 * <p><b>Purpose</b>: Define the contract for approval step handlers in an extensible approval
 * chain. Handlers evaluate booking requests based on requester role, facility constraints, and
 * institutional policies, and delegate to the next handler when additional approvals are required.
 *
 * <p><b>Design Pattern</b>: Chain of Responsibility. Each handler is responsible for:
 * <ul>
 *   <li>Evaluating whether it can process the approval (role/context match)
 *   <li>Making an approval decision (approve, reject, or defer to next handler)
 *   <li>Delegating to the next handler if further approval is required
 * </ul>
 *
 * <p><b>Approval Rules Enforced</b> (from FR-014, FR-015, FR-016, FR-017):
 * <ul>
 *   <li><b>USER bookings</b>: LECTURER approval required → then ADMIN approval
 *   <li><b>LECTURER bookings</b>: auto-approve unless high-capacity facility (>threshold)
 *   <li><b>High-capacity halls</b>: additional FACILITY_MANAGER sign-off required
 *   <li><b>ADMIN bookings</b>: bypass approval (immediate approval)
 * </ul>
 *
 * <p><b>Typical Chain Order</b>:
 * <ol>
 *   <li>LecturerApprovalHandler (applies FR-015, FR-016)
 *   <li>FacilityManagerApprovalHandler (applies FR-017 conditional high-capacity check)
 *   <li>AdminApprovalHandler (applies final ADMIN sign-off)
 * </ol>
 *
 * <p><b>Example Usage</b>:
 * <pre>{@code
 * ApprovalHandler chain = new LecturerApprovalHandler()
 *     .setNext(new FacilityManagerApprovalHandler()
 *         .setNext(new AdminApprovalHandler()));
 *
 * ApprovalDecision decision = chain.handle(booking, requester, facility);
 * // Handle decision: APPROVED, REJECTED, PENDING
 * }</pre>
 *
 * <p><b>State Management</b>: Each handler is stateless and reusable; all context (booking,
 * requester, facility) is passed through method parameters to enable concurrent invocations.
 *
 * @see ApprovalDecision for the approval decision result
 */
public interface ApprovalHandler {

  /**
   * Process the approval step for a booking request.
   *
   * <p>Implementations MUST:
   * <ul>
   *   <li>Evaluate whether this handler's role/context applies to the request
   *   <li>Make a decision: APPROVED, REJECTED, or PENDING (defer to next)
   *   <li>If PENDING, delegate to the next handler if registered
   *   <li>Return the decision (or propagated decision from next handler)
   *   <li>Be idempotent: repeated calls with same inputs yield same output
   * </ul>
   *
   * @param booking the booking request under approval (provides attendees, facility reference,
   *     requester, etc.)
   * @param requester the user submitting the booking request
   * @param facility the facility being booked
   * @return an {@link ApprovalDecision} with status (APPROVED/REJECTED/PENDING), approver role,
   *     and decision notes
   * @throws IllegalArgumentException if required context is missing (booking, requester, or
   *     facility is null)
   */
  ApprovalDecision handle(Booking booking, User requester, Facility facility);

  /**
   * Determine whether this handler's approval criteria apply to the current booking context.
   *
   * <p>Implementations MUST check role, facility type, capacity, or other context-specific
   * conditions to determine if this handler should process the approval or defer to the next
   * handler.
   *
   * <p>Example: A LECTURER-approval handler returns true only if the requester is a USER role
   * (not LECTURER or ADMIN).
   *
   * @param booking the booking request
   * @param requester the user submitting the booking
   * @param facility the facility being booked
   * @return true if this handler's approval applies; false if it should defer to next handler
   */
  boolean canHandle(Booking booking, User requester, Facility facility);

  /**
   * Register the next handler in the approval chain.
   *
   * <p>This method establishes the chain link; handlers invoke {@link #handle(Booking, User,
   * Facility)} on the next handler when they determine further approval is required (decision is
   * PENDING).
   *
   * @param nextHandler the next approval handler in the chain
   * @return this handler for fluent chaining: {@code handler.setNext(next1).setNext(next2)}
   */
  ApprovalHandler setNext(ApprovalHandler nextHandler);

  /**
   * Retrieve the next handler in the approval chain.
   *
   * @return the next handler, or null if this is the terminal handler
   */
  ApprovalHandler getNext();

  /**
   * Get the handler's role identifier for decision attribution.
   *
   * @return a descriptive role name (e.g., "LECTURER", "FACILITY_MANAGER", "ADMIN") used in
   *     approval decision records and audit logs
   */
  String getApprovalRole();
}
