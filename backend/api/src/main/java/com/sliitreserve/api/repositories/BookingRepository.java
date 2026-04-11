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
     * Count bookings for a user within a specific week (campus local timezone).
     * Week is defined as Monday-Sunday (ISO 8601).
     * Counts only APPROVED and PENDING bookings (not REJECTED or CANCELLED).
     * 
     * @param bookedForUserId User ID to count bookings for
     * @param weekStartDate Monday of the week (inclusive)
     * @param weekEndDate Sunday of the week (inclusive)
     * @return Number of bookings in the week
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.bookedFor.id = :bookedForUserId " +
           "AND b.bookingDate >= :weekStart AND b.bookingDate <= :weekEnd " +
           "AND b.status IN ('PENDING', 'APPROVED')")
    long countWeeklyBookings(
            @Param("bookedForUserId") UUID bookedForUserId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);

    /**
     * Count bookings for a user within a specific month (campus local timezone).
     * Month is defined by calendar month year.
     * Counts only APPROVED and PENDING bookings (not REJECTED or CANCELLED).
     * 
     * @param bookedForUserId User ID to count bookings for
     * @param monthStartDate First day of the month (inclusive)
     * @param monthEndDate Last day of the month (inclusive)
     * @return Number of bookings in the month
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.bookedFor.id = :bookedForUserId " +
           "AND b.bookingDate >= :monthStart AND b.bookingDate <= :monthEnd " +
           "AND b.status IN ('PENDING', 'APPROVED')")
    long countMonthlyBookings(
            @Param("bookedForUserId") UUID bookedForUserId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);
}
