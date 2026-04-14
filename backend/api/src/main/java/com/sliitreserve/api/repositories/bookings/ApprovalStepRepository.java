package com.sliitreserve.api.repositories.bookings;

import com.sliitreserve.api.entities.booking.ApprovalStep;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.repositories.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApprovalStepRepository extends BaseRepository<ApprovalStep, UUID> {

    List<ApprovalStep> findByBookingOrderByStepOrderAsc(Booking booking);

    @Query("SELECT a FROM ApprovalStep a WHERE a.booking.id = :bookingId ORDER BY a.stepOrder ASC")
    List<ApprovalStep> findApprovalHistoryByBookingId(@Param("bookingId") UUID bookingId);
}
