package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.booking.CheckInRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for CheckInRecord entity.
 */
public interface CheckInRepository extends BaseRepository<CheckInRecord, UUID> {
    
    /**
     * Find all check-in records for a specific booking.
     * 
     * @param bookingId Booking ID
     * @return List of check-in records for the booking (typically 0 or 1)
     */
    List<CheckInRecord> findByBooking_Id(UUID bookingId);
    
    /**
     * Find the latest check-in record for a booking.
     * Useful for determining the most recent check-in timestamp if multiple records exist.
     * 
     * @param bookingId Booking ID
     * @return Optional containing the most recent check-in, or empty if none exists
     */
    @Query("SELECT c FROM CheckInRecord c WHERE c.booking.id = :bookingId ORDER BY c.checkedInAt DESC")
    List<CheckInRecord> findAllByBookingIdOrderByCheckInDesc(@Param("bookingId") UUID bookingId);
    
    /**
     * Check if a check-in record exists for a booking.
     * 
     * @param bookingId Booking ID
     * @return true if at least one check-in record exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CheckInRecord c WHERE c.booking.id = :bookingId")
    boolean existsByBookingId(@Param("bookingId") UUID bookingId);
}
