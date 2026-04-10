package com.sliitreserve.api.workflow.approval;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.facility.Facility;
import lombok.extern.slf4j.Slf4j;

/**
 * Facility Manager Approval Handler (T055)
 * 
 * Purpose: Handle conditional approval for high-capacity facility bookings.
 * 
 * Approval Logic (FR-017):
 * - If facility capacity >= HIGH_CAPACITY_THRESHOLD: require FACILITY_MANAGER approval
 * - Otherwise: defer to next handler (skip this approval)
 * - If requester is ADMIN: defer to next handler
 * 
 * High-Capacity Threshold: 200+ attendees or facility capacity
 * 
 * This handler sits in the middle of the chain and conditionally requires approval
 * based on facility capacity constraints.
 * 
 * @see ApprovalHandler for the interface contract
 * @see LecturerApprovalHandler for the previous handler
 * @see AdminApprovalHandler for the next handler
 */
@Slf4j
public class FacilityManagerApprovalHandler implements ApprovalHandler {

    private ApprovalHandler nextHandler;
    private static final String APPROVAL_ROLE = "FACILITY_MANAGER";
    private static final int HIGH_CAPACITY_THRESHOLD = 200;

    @Override
    public ApprovalDecision handle(Booking booking, User requester, Facility facility) {
        if (booking == null || requester == null || facility == null) {
            throw new IllegalArgumentException("Booking, requester, and facility must not be null");
        }

        log.debug("Facility manager approval handler processing booking {} for requester {}", 
                  booking.getId(), requester.getId());

        // If this handler cannot process, defer to next
        if (!canHandle(booking, requester, facility)) {
            if (nextHandler != null) {
                return nextHandler.handle(booking, requester, facility);
            }
            // If no next handler, approve by default
            return ApprovalDecision.builder()
                    .status(ApprovalStatus.APPROVED)
                    .approverRole(APPROVAL_ROLE)
                    .note("No further approval required")
                    .build();
        }

        // Check if high-capacity approval is required
        if (facility.getCapacity() >= HIGH_CAPACITY_THRESHOLD) {
            log.debug("High-capacity facility approval required (capacity: {})", facility.getCapacity());
            
            // For now, auto-approve at facility manager level
            // In real implementation, this would check if an actual facility manager approves
            ApprovalDecision facilityManagerApproval = ApprovalDecision.builder()
                    .status(ApprovalStatus.APPROVED)
                    .approverRole(APPROVAL_ROLE)
                    .note("Facility manager approval granted for high-capacity booking")
                    .build();

            // Defer to next handler for further approvals
            if (nextHandler != null) {
                return nextHandler.handle(booking, requester, facility);
            }
            return facilityManagerApproval;
        }

        // Normal capacity: no facility manager approval needed, defer to next handler
        log.debug("Normal capacity facility - no facility manager approval required");
        if (nextHandler != null) {
            return nextHandler.handle(booking, requester, facility);
        }

        return ApprovalDecision.builder()
                .status(ApprovalStatus.APPROVED)
                .approverRole(APPROVAL_ROLE)
                .build();
    }

    @Override
    public boolean canHandle(Booking booking, User requester, Facility facility) {
        // This handler processes any booking where facility is high-capacity
        // and requester is not ADMIN (ADMIN bookings bypass all approvals)
        return !requester.getRoles().contains(Role.ADMIN) && 
               facility.getCapacity() >= HIGH_CAPACITY_THRESHOLD;
    }

    @Override
    public ApprovalHandler setNext(ApprovalHandler nextHandler) {
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
