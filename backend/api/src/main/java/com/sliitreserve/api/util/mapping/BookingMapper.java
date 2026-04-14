package com.sliitreserve.api.util.mapping;

import com.sliitreserve.api.dto.bookings.BookingRequestDTO;
import com.sliitreserve.api.dto.bookings.BookingResponseDTO;
import com.sliitreserve.api.entities.booking.Booking;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Booking entities and DTOs.
 */
@Component
public class BookingMapper implements BaseMapper<Booking, BookingRequestDTO, BookingResponseDTO> {

    @Override
    public BookingResponseDTO toResponseDTO(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setFacilityId(booking.getFacility() != null ? booking.getFacility().getId() : null);
        dto.setRequestedByUserId(booking.getRequestedBy() != null ? booking.getRequestedBy().getId() : null);
        dto.setBookedForUserId(booking.getBookedFor() != null ? booking.getBookedFor().getId() : null);
        dto.setBookingDate(booking.getBookingDate());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setPurpose(booking.getPurpose());
        dto.setAttendees(booking.getAttendees());
        dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
        dto.setRecurrenceRule(booking.getRecurrenceRule());
        dto.setIsRecurringMaster(booking.isRecurringMaster());
        dto.setTimezone(booking.getTimezone());
        dto.setVersion(booking.getVersion());
        return dto;
    }

    @Override
    public Booking toEntity(BookingRequestDTO dto) {
        // Typically creation is handled by the specialized BookingBuilder in BookingService. 
        // Returning a new generic instance just to satisfy the BaseMapper if required.
        return new Booking();
    }

    @Override
    public Booking updateEntity(BookingRequestDTO dto, Booking entity) {
        // Will be used for UPDATE operations if we support partial updates
        return entity;
    }
}
