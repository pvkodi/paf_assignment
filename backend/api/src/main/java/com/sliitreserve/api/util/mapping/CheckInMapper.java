package com.sliitreserve.api.util.mapping;

import com.sliitreserve.api.dto.bookings.CheckInRequestDTO;
import com.sliitreserve.api.dto.bookings.CheckInResponseDTO;
import com.sliitreserve.api.entities.booking.CheckInRecord;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between CheckInRecord entities and DTOs.
 */
@Component
public class CheckInMapper implements BaseMapper<CheckInRecord, CheckInRequestDTO, CheckInResponseDTO> {

    @Override
    public CheckInResponseDTO toResponseDTO(CheckInRecord checkInRecord) {
        if (checkInRecord == null) {
            return null;
        }

        CheckInResponseDTO dto = new CheckInResponseDTO();
        dto.setId(checkInRecord.getId());
        dto.setBookingId(checkInRecord.getBooking() != null ? checkInRecord.getBooking().getId() : null);
        dto.setMethod(checkInRecord.getMethod() != null ? checkInRecord.getMethod().name() : null);
        dto.setCheckedInByUserId(checkInRecord.getCheckedInBy() != null ? checkInRecord.getCheckedInBy().getId() : null);
        dto.setCheckedInAt(checkInRecord.getCheckedInAt());
        dto.setCreatedAt(checkInRecord.getCreatedAt());
        return dto;
    }

    @Override
    public CheckInRecord toEntity(CheckInRequestDTO dto) {
        // Not used for check-in creation (service handles it)
        return new CheckInRecord();
    }

    @Override
    public CheckInRecord updateEntity(CheckInRequestDTO dto, CheckInRecord entity) {
        // Check-in records are immutable once created
        return entity;
    }
}
