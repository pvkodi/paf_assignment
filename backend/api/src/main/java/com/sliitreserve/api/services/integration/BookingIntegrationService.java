package com.sliitreserve.api.services.integration;

import com.sliitreserve.api.dto.integration.BookingDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Integration boundary for Booking module.
 * EXCLUDED: booking workflow, conflict detection, and booking retrieval logic are handled by dependent module.
 */
public interface BookingIntegrationService {

    double getBookedHours(UUID facilityId, LocalDateTime start, LocalDateTime end);

    List<BookingDTO> getBookingsForFacility(UUID facilityId, LocalDateTime start, LocalDateTime end);
}
