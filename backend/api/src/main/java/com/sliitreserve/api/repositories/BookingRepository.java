package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Booking entity.
 */
public interface BookingRepository extends BaseRepository<Booking, UUID> {
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.facility.id = :facilityId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.status IN (:statuses) " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean existsOverlappingBooking(
            @Param("facilityId") UUID facilityId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<BookingStatus> statuses);
    
    List<Booking> findByRequestedBy_Id(UUID userId);
    
    /**
     * Find all bookings by status.
     * @param status The booking status to filter by
     * @return List of bookings with the given status
     */
    List<Booking> findByStatus(BookingStatus status);
}
