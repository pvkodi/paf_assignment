package com.sliitreserve.api.workflow.approval;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.facility.Facility;
import lombok.extern.slf4j.Slf4j;

/**
 * Lecturer Approval Handler (T055)
 * 
 * Purpose: Handle approval step for bookings submitted by USER role users,
 * and auto-approve bookings submitted by LECTURER role for normal-capacity facilities.
 * 
 * Approval Logic (FR-015, FR-016):
 * - If requester is USER: evaluate LECTURER approval; if approved, defer to next handler
 * - If requester is LECTURER: auto-approve unless facility is high-capacity (defer to FacilityManager)
 * - If requester is ADMIN: defer to next handler (bypass lecturer approval)
 * 
 * High-Capacity Threshold: 200+ attendees or facility capacity
 * 
 * @see ApprovalHandler for the interface contract
 * @see FacilityManagerApprovalHandler for the next handler
 */
@Slf4j
public class LecturerApprovalHandler implements ApprovalHandler {

    private ApprovalHandler nextHandler;
    private static final String APPROVAL_ROLE = "LECTURER";
    private static final int HIGH_CAPACITY_THRESHOLD = 200;

    @Override
    public ApprovalDecision handle(Booking booking, User requester, Facility facility) {
        if (booking == null || requester == null || facility == null) {
            throw new IllegalArgumentException("Booking, requester, and facility must not be null");
        }

        log.debug("Lecturer approval handler processing booking {} for requester {}", 
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

        // Handle USER bookings: require lecturer approval
        if (requester.getRoles().contains(Role.USER)) {
            log.debug("USER booking requires LECTURER approval");
            // For now, auto-approve at lecturer level (in real implementation, 
            // this would check if an actual lecturer approves)
            ApprovalDecision lecturerApproval = ApprovalDecision.builder()
                    .status(ApprovalStatus.APPROVED)
                    .approverRole(APPROVAL_ROLE)
                    .note("Lecturer approval granted")
                    .build();

            // Defer to next handler for further approvals
            if (nextHandler != null) {
                return nextHandler.handle(booking, requester, facility);
            }
            return lecturerApproval;
        }

        // Handle LECTURER bookings: auto-approve or defer to FacilityManager if high-capacity
        if (requester.getRoles().contains(Role.LECTURER)) {
            log.debug("LECTURER booking - checking if high-capacity approval required");
            
            // Check if facility is high-capacity
            if (facility.getCapacity() >= HIGH_CAPACITY_THRESHOLD) {
                log.debug("High-capacity facility detected; deferring to FacilityManager");
                // Defer to next handler (FacilityManager)
                if (nextHandler != null) {
                    return nextHandler.handle(booking, requester, facility);
                }
            }

            // Normal capacity: auto-approve
            log.debug("LECTURER booking on normal-capacity facility - auto-approving");
            return ApprovalDecision.builder()
                    .status(ApprovalStatus.APPROVED)
                    .approverRole(APPROVAL_ROLE)
                    .note("Auto-approved for LECTURER on normal-capacity facility")
                    .build();
        }

        // Default: defer to next handler
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
        // This handler can process USER or LECTURER bookings
        return requester.getRoles().contains(Role.USER) || 
               requester.getRoles().contains(Role.LECTURER);
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
