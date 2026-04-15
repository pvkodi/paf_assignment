package com.sliitreserve.api.repositories.bookings;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.BaseRepository;
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
     * Find all bookings with a specific status.
     */
    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByFacility_IdAndBookingDateAndStatusIn(
           UUID facilityId,
           LocalDate bookingDate,
           List<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.bookedFor.id = :bookedForUserId " +
           "AND b.bookingDate >= :weekStart AND b.bookingDate <= :weekEnd " +
           "AND b.status IN (com.sliitreserve.api.entities.booking.BookingStatus.PENDING, com.sliitreserve.api.entities.booking.BookingStatus.APPROVED)")
    long countWeeklyBookings(
            @Param("bookedForUserId") UUID bookedForUserId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);

    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.bookedFor.id = :bookedForUserId " +
           "AND b.bookingDate >= :monthStart AND b.bookingDate <= :monthEnd " +
           "AND b.status IN (com.sliitreserve.api.entities.booking.BookingStatus.PENDING, com.sliitreserve.api.entities.booking.BookingStatus.APPROVED)")
    long countMonthlyBookings(
            @Param("bookedForUserId") UUID bookedForUserId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);

    @Query(value = "SELECT COUNT(*) FROM booking b " +
           "WHERE b.facility_id = :facilityId " +
           "AND b.status IN ('APPROVED', 'PENDING') " +
           "AND b.booking_date = DATE(:now) " +
           "AND b.start_time <= TIME(:now) " +
           "AND b.end_time > TIME(:now)", nativeQuery = true)
    long countActiveBookings(@Param("facilityId") String facilityId,
                             @Param("now") java.time.LocalDateTime now);

    @Query(value = "SELECT MIN(b.end_time) FROM booking b " +
           "WHERE b.facility_id = :facilityId " +
           "AND b.status IN ('APPROVED', 'PENDING') " +
           "AND b.end_time > TIME(:now)", nativeQuery = true)
    java.util.Optional<java.time.LocalTime> findNextAvailabilityTime(
            @Param("facilityId") String facilityId,
            @Param("now") java.time.LocalDateTime now);

    @Query(value = "SELECT * FROM booking b " +
           "WHERE b.facility_id = :facilityId " +
           "AND b.status IN ('APPROVED', 'PENDING') " +
           "AND b.booking_date >= DATE(:now) " +
           "AND (b.booking_date > DATE(:now) OR b.start_time > TIME(:now)) " +
           "ORDER BY b.booking_date ASC, b.start_time ASC " +
           "LIMIT 1", nativeQuery = true)
    java.util.Optional<Booking> findNextBooking(@Param("facilityId") String facilityId,
                                                @Param("now") java.time.LocalDateTime now);

    @Query(value = "SELECT COUNT(*) FROM booking b " +
           "WHERE b.facility_id = :facilityId " +
           "AND b.status IN ('APPROVED', 'PENDING') " +
           "AND b.start_time < TIME(:endTime) " +
           "AND b.end_time > TIME(:startTime) " +
           "AND DATE(b.booking_date) = DATE(:startTime)", nativeQuery = true)
    long countBookingsBetween(@Param("facilityId") String facilityId,
                             @Param("startTime") java.time.LocalDateTime startTime,
                             @Param("endTime") java.time.LocalDateTime endTime);

    @Query("SELECT f FROM Facility f " +
           "WHERE f.type = :facilityType " +
           "AND f.capacity >= :capacity " +
           "AND f.status = 'ACTIVE'")
    java.util.List<Facility> findAvailableFacilities(
            @Param("facilityType") Facility.FacilityType facilityType,
            @Param("capacity") Integer capacity);

    /**
     * Count concurrent bookings for a user at a specific time slot.
     * Concurrent bookings are those that overlap with the proposed time on the same date.
     * Counts only APPROVED and PENDING bookings.
     *
     * @param userId User ID to count bookings for
     * @param bookingDate Date of the booking
     * @param startTime Start time of the proposed booking
     * @param endTime End time of the proposed booking
     * @return Number of concurrent bookings
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.bookedFor.id = :userId " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.startTime < :endTime AND b.endTime > :startTime " +
           "AND b.status IN (com.sliitreserve.api.entities.booking.BookingStatus.PENDING, com.sliitreserve.api.entities.booking.BookingStatus.APPROVED)")
    long countConcurrentBookings(
            @Param("userId") UUID userId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
