package com.sliitreserve.api.services.integration;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Integration boundary for Maintenance module.
 * EXCLUDED: maintenance lifecycle and SLA orchestration are handled by dependent module.
 */
public interface MaintenanceIntegrationService {

    boolean isFacilityUnderMaintenance(UUID facilityId, LocalDateTime start, LocalDateTime end);
}
