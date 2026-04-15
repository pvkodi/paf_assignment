package com.sliitreserve.api.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Booking projection used by facility module integration boundaries.
 * EXCLUDED: lifecycle and validation are handled by the Booking module.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {

    private UUID bookingId;
    private UUID facilityId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
}
