package com.sliitreserve.api.services.integration;

import com.sliitreserve.api.dto.integration.BookingDTO;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Stub implementation used by Facilities module until Booking module integration is wired.
 * EXCLUDED: handled by dependent module.
 */
@Service
@Primary
public class StubBookingIntegrationService implements BookingIntegrationService {

    @Override
    public double getBookedHours(UUID facilityId, LocalDateTime start, LocalDateTime end) {
        return 0.0;
    }

    @Override
    public List<BookingDTO> getBookingsForFacility(UUID facilityId, LocalDateTime start, LocalDateTime end) {
        return Collections.emptyList();
    }
}
