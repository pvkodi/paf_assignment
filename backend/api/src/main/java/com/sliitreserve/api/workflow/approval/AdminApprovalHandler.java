package com.sliitreserve.api.workflow.approval;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.facility.Facility;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Approval Handler (T055)
 * 
 * Purpose: Handle final approval step by ADMIN role, or bypass entire approval chain
 * for bookings initiated by ADMIN users.
 * 
 * Approval Logic (FR-001, FR-002, FR-014):
 * - If requester is ADMIN: return APPROVED immediately (bypass all previous approvals)
 * - If requester is not ADMIN: require ADMIN approval; for implementation, auto-approve
 *   (in production, would check if an actual admin approves)
 * 
 * This handler is terminal in the approval chain. It always produces a final decision
 * (APPROVED or REJECTED) without deferring further.
 * 
 * @see ApprovalHandler for the interface contract
 * @see FacilityManagerApprovalHandler for the previous handler
 */
@Slf4j
public class AdminApprovalHandler implements ApprovalHandler {

    private ApprovalHandler nextHandler;
    private static final String APPROVAL_ROLE = "ADMIN";

    @Override
    public ApprovalDecision handle(Booking booking, User requester, Facility facility) {
        if (booking == null || requester == null || facility == null) {
            throw new IllegalArgumentException("Booking, requester, and facility must not be null");
        }

        log.debug("Admin approval handler processing booking {} for requester {}", 
                  booking.getId(), requester.getId());

        // If requester is ADMIN: auto-approve immediately (bypass all approvals)
        if (requester.getRoles().contains(Role.ADMIN)) {
            log.debug("ADMIN requester - auto-approving booking");
            return ApprovalDecision.builder()
                    .status(ApprovalStatus.APPROVED)
                    .approverRole(APPROVAL_ROLE)
                    .note("Auto-approved for ADMIN requester")
                    .build();
        }

        // For non-ADMIN requesters: require final ADMIN approval
        log.debug("Final ADMIN approval required for non-ADMIN requester");
        
        // Auto-approve at admin level (in real implementation, 
        // this would check if an actual admin approves)
        ApprovalDecision adminApproval = ApprovalDecision.builder()
                .status(ApprovalStatus.APPROVED)
                .approverRole(APPROVAL_ROLE)
                .note("Admin approval granted")
                .build();

        // Admin handler is terminal - do not defer further
        return adminApproval;
    }

    @Override
    public boolean canHandle(Booking booking, User requester, Facility facility) {
        // Admin handler can always handle - it's the terminal handler
        return true;
    }

    @Override
    public ApprovalHandler setNext(ApprovalHandler nextHandler) {
        // Admin handler is terminal - ignore setNext as it should not defer further
        this.nextHandler = nextHandler;
        return this;
    }

    @Override
    public ApprovalHandler getNext() {
        return nextHandler;
    }

    @Override
    public String getApprovalRole() {
        return APPROVAL_ROLE;
    }
}
