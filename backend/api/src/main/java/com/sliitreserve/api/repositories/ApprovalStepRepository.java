package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.booking.ApprovalStep;
import com.sliitreserve.api.entities.booking.Booking;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ApprovalStep entity (T056).
 * 
 * Purpose: Provide data access operations for approval workflow history.
 * 
 * Supports:
 * - Recording approval steps with approver details, decisions, and timestamps
 * - Retrieving approval workflow history by booking
 * - Querying approval steps by approver role or decision status
 * 
 * @see ApprovalStep for the entity
 * @see Booking for the booking reference
 */
@Repository
public interface ApprovalStepRepository extends BaseRepository<ApprovalStep, UUID> {

    /**
     * Find all approval steps for a booking, ordered by stepOrder.
     * 
     * @param booking the booking to fetch approval history for
     * @return list of approval steps ordered by stepOrder (ASC)
     */
    List<ApprovalStep> findByBookingOrderByStepOrderAsc(Booking booking);

    /**
     * Find all approval steps for a booking by ID, ordered by stepOrder.
     * 
     * @param bookingId the booking ID
     * @return list of approval steps
     */
    @Query("SELECT a FROM ApprovalStep a WHERE a.booking.id = :bookingId ORDER BY a.stepOrder ASC")
    List<ApprovalStep> findApprovalHistoryByBookingId(@Param("bookingId") UUID bookingId);
}
