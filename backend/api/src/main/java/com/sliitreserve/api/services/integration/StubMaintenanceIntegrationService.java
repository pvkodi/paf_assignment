package com.sliitreserve.api.services.integration;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stub implementation used by Facilities module until Maintenance module integration is wired.
 * EXCLUDED: handled by dependent module.
 */
@Service
@Primary
public class StubMaintenanceIntegrationService implements MaintenanceIntegrationService {

    @Override
    public boolean isFacilityUnderMaintenance(UUID facilityId, LocalDateTime start, LocalDateTime end) {
        return false;
    }
}
