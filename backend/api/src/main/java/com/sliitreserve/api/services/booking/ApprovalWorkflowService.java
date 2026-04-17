package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.ApprovalStep;
import com.sliitreserve.api.entities.booking.ApprovalStepDecision;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.strategy.quota.RolePolicyResolver;
import com.sliitreserve.api.workflow.approval.AdminApprovalHandler;
import com.sliitreserve.api.workflow.approval.ApprovalDecision;
import com.sliitreserve.api.workflow.approval.ApprovalHandler;
import com.sliitreserve.api.workflow.approval.ApprovalStatus;
import com.sliitreserve.api.workflow.approval.FacilityManagerApprovalHandler;
import com.sliitreserve.api.workflow.approval.LecturerApprovalHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Approval Workflow Service (T056)
 * 
 * Purpose: Orchestrate the booking approval workflow by chaining approval handlers
 * according to user role and facility constraints.
 * 
 * Responsibilities:
 * - Determine the appropriate approval chain based on booking context
 * - Invoke handlers in sequence (Chain of Responsibility pattern)
 * - Record approval steps and decisions in the database
 * - Update booking status based on approval outcomes
 * - Handle multi-role policy resolution (most permissive role)
 * 
 * Approval Rules (FR-014 to FR-017):
 * 1. USER bookings: 2-step approval (LECTURER → ADMIN)
 * 2. LECTURER bookings: Auto-approve OR FACILITY_MANAGER approval if high-capacity
 * 3. High-capacity facilities (>threshold): Additional FACILITY_MANAGER sign-off
 * 4. ADMIN bookings: Bypass entire workflow (auto-approve)
 * 
 * @see ApprovalHandler for the approval handler interface
 * @see ApprovalDecision for approval outcome
 * @see ApprovalStep for approval history recording
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalWorkflowService {

    private final ApprovalStepRepository approvalStepRepository;
    private final BookingRepository bookingRepository;
    private final RolePolicyResolver rolePolicyResolver;

    /**
     * Initiate the approval workflow for a booking request.
     * 
     * This method:
     * 1. Validates the booking context (requester, facility, dates)
     * 2. Determines the most permissive role for multi-role users
     * 3. Builds the appropriate approval chain based on role and facility constraints
     * 4. Invokes the chain to obtain approval decisions
     * 5. Records approval steps in the database
     * 6. Updates the booking status based on final decision
     * 
     * @param booking the booking request to approve
     * @return the final approval decision and booking status
     * @throws IllegalArgumentException if booking, requester, or facility is null
     */
    public ApprovalDecision initiateApproval(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        if (booking.getRequestedBy() == null) {
            throw new IllegalArgumentException("Booking requester cannot be null");
        }
        if (booking.getFacility() == null) {
            throw new IllegalArgumentException("Booking facility cannot be null");
        }

        log.info("Initiating approval workflow for booking {} by user {}", 
                 booking.getId(), booking.getRequestedBy().getId());

        User requester = booking.getRequestedBy();
        Facility facility = booking.getFacility();

        // Build the approval chain
        ApprovalHandler approvalChain = buildApprovalChain(booking, requester, facility);

        // Invoke the chain to get final decision
        ApprovalDecision finalDecision = approvalChain.handle(booking, requester, facility);

        log.debug("Approval workflow result: status={}, approver={}", 
                  finalDecision.getStatus(), finalDecision.getApproverRole());

        // Update booking status based on decision
        if (finalDecision.isApproved()) {
            booking.setStatus(BookingStatus.APPROVED);
            log.info("Booking {} approved", booking.getId());
        } else if (finalDecision.isRejected()) {
            booking.setStatus(BookingStatus.REJECTED);
            log.info("Booking {} rejected", booking.getId());
        } else {
            booking.setStatus(BookingStatus.PENDING);
            log.info("Booking {} pending further approval", booking.getId());
        }

        // Save updated booking
        bookingRepository.save(booking);

        // Record approval step
        recordApprovalStep(booking, 1, finalDecision);

        return finalDecision;
    }

    /**
     * Process a single approval step decision.
     * 
     * This method:
     * 1. Records the decision made by the approver
     * 2. Updates the approval step entity with decision details
     * 3. Evaluates whether the booking is approved, rejected, or pending
     * 4. Updates the booking status if decision is terminal
     * 
     * @param approvalStep the approval step to process
     * @param decision the decision made by the handler
     * @return the updated booking entity
     */
    public Booking processApprovalStep(ApprovalStep approvalStep, ApprovalDecision decision) {
        if (approvalStep == null) {
            throw new IllegalArgumentException("Approval step cannot be null");
        }
        if (decision == null) {
            throw new IllegalArgumentException("Approval decision cannot be null");
        }

        Booking booking = approvalStep.getBooking();
        
        log.debug("Processing approval step {} for booking {}: decision={}", 
                  approvalStep.getStepOrder(), booking.getId(), decision.getStatus());

        // Update approval step with decision details
        approvalStep.setDecision(ApprovalStepDecision.valueOf(decision.getStatus().name()));
        approvalStep.setDecidedAt(LocalDateTime.now());
        approvalStep.setNote(decision.getNote());

        approvalStepRepository.save(approvalStep);

        // Update booking status if decision is terminal
        if (decision.isTerminal()) {
            if (decision.isApproved()) {
                booking.setStatus(BookingStatus.APPROVED);
            } else if (decision.isRejected()) {
                booking.setStatus(BookingStatus.REJECTED);
            }
            bookingRepository.save(booking);
        }

        return booking;
    }

    /**
     * Build the appropriate approval chain for a booking.
     * 
     * This method constructs the chain based on:
     * - Requester's most permissive role
     * - Facility capacity (high-capacity threshold)
     * - Institutional approval policies
     * 
     * Chain Order:
     * 1. LecturerApprovalHandler (handles USER and LECTURER bookings)
     * 2. FacilityManagerApprovalHandler (conditional high-capacity sign-off)
     * 3. AdminApprovalHandler (final approval or bypass)
     * 
     * @param booking the booking to build approval chain for
     * @param requester the user requesting the booking
     * @param facility the facility being booked
     * @return the head of the approval handler chain
     */
    protected ApprovalHandler buildApprovalChain(Booking booking, User requester, Facility facility) {
        log.debug("Building approval chain for booking {} with requester roles: {}", 
                  booking.getId(), requester.getRoles());

        // Build chain: Lecturer → FacilityManager → Admin
        ApprovalHandler lecturerHandler = new LecturerApprovalHandler();
        ApprovalHandler facilityManagerHandler = new FacilityManagerApprovalHandler();
        ApprovalHandler adminHandler = new AdminApprovalHandler();

        lecturerHandler.setNext(facilityManagerHandler);
        facilityManagerHandler.setNext(adminHandler);

        return lecturerHandler;
    }

    /**
     * Record approval step in the database.
     * 
     * Creates an ApprovalStep entity linked to the booking with:
     * - Step order (sequence in chain)
     * - Approver role
     * - Decision status
     * - Decision timestamp and user (if applicable)
     * - Optional note/justification
     * 
     * @param booking the booking being approved
     * @param stepOrder the sequence in the approval chain
     * @param decision the approval decision
     * @return the saved approval step entity
     */
    protected ApprovalStep recordApprovalStep(Booking booking, int stepOrder, ApprovalDecision decision) {
        if (booking == null || decision == null) {
            throw new IllegalArgumentException("Booking and decision cannot be null");
        }

        log.debug("Recording approval step {} for booking {}: role={}, decision={}", 
                  stepOrder, booking.getId(), decision.getApproverRole(), decision.getStatus());

        // Parse approver role from decision
        Role approverRole = parseApproverRole(decision.getApproverRole());

        ApprovalStep step = ApprovalStep.builder()
                .booking(booking)
                .stepOrder(stepOrder)
                .approverRole(approverRole)
                .decision(ApprovalStepDecision.valueOf(decision.getStatus().name()))
                .decidedAt(LocalDateTime.now())
                .note(decision.getNote())
                .build();

        ApprovalStep saved = approvalStepRepository.save(step);
        log.debug("Approval step {} created with ID: {}", stepOrder, saved.getId());

        return saved;
    }

    /**
     * Retrieve all approval steps for a booking in order.
     * 
     * Used for audit trails, progress tracking, and approval history visibility.
     * 
     * @param booking the booking to fetch approval history for
     * @return list of approval steps ordered by stepOrder
     */
    public List<ApprovalStep> getApprovalHistory(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        return approvalStepRepository.findByBookingOrderByStepOrderAsc(booking);
    }

    /**
     * Parse approver role string to Role enum.
     * 
     * Maps approval handler role names to Role enum values.
     * 
     * @param roleName the role name from approval decision
     * @return corresponding Role enum
     */
    private Role parseApproverRole(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return Role.ADMIN; // Default
        }

        try {
            return Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown approver role: {}, defaulting to ADMIN", roleName);
            return Role.ADMIN;
        }
    }

    /**
     * Check if a booking requires approval (i.e., is not from ADMIN).
     * 
     * ADMIN bookings bypass approval; all other bookings require approval workflow.
     * 
     * @param booking the booking to check
     * @return true if approval is required, false if should auto-approve
     */
    public boolean isApprovalRequired(Booking booking) {
        if (booking == null || booking.getRequestedBy() == null) {
            return true;
        }
        return !booking.getRequestedBy().getRoles().contains(Role.ADMIN);
    }

    /**
     * Get the current approval status for a booking.
     * 
     * Useful for displaying workflow progress to end users.
     * 
     * @param booking the booking to check
     * @return current booking status
     */
    public BookingStatus getCurrentStatus(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        return booking.getStatus();
    }
}

