package com.sliitreserve.api.repositories.bookings;

import com.sliitreserve.api.entities.booking.CheckInRecord;
import com.sliitreserve.api.repositories.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CheckInRepository extends BaseRepository<CheckInRecord, UUID> {

    List<CheckInRecord> findByBooking_Id(UUID bookingId);

    @Query("SELECT c FROM CheckInRecord c WHERE c.booking.id = :bookingId ORDER BY c.checkedInAt DESC")
    List<CheckInRecord> findAllByBookingIdOrderByCheckInDesc(@Param("bookingId") UUID bookingId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CheckInRecord c WHERE c.booking.id = :bookingId")
    boolean existsByBookingId(@Param("bookingId") UUID bookingId);
}
