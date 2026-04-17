package com.sliitreserve.api.strategy.quota;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Bootstrap component for quota strategy registration.
 * 
 * This component is initialized at application startup and registers
 * all available quota strategies with the RolePolicyResolver.
 * 
 * Concrete strategy implementations are added by T053.
 * 
 * Registration flow:
 * 1. T053 creates StudentQuotaStrategy, LecturerQuotaStrategy, AdminQuotaStrategy
 * 2. They are registered as Spring @Component beans with specific names
 * 3. This bootstrap component receives them via @Autowired
 * 4. @PostConstruct method registers each with RolePolicyResolver
 * 5. RolePolicyResolver is ready for policy resolution
 * 
 * If a strategy bean is not found (not yet implemented), bootstrap logs a warning
 * and allows the application to continue. This enables gradual implementation.
 */
@Slf4j
@Component
public class QuotaStrategyBootstrap {

    @Autowired
    private RolePolicyResolver rolePolicyResolver;

    /**
     * Initialize and register quota strategies at application startup.
     * Called by Spring after all beans are constructed.
     * 
     * Attempt to autowire concrete strategy implementations (created in T053).
     * This uses optional injection to avoid failures if strategies are not yet implemented.
     */
    @Autowired(required = false)
    private StudentQuotaStrategy studentStrategy;

    @Autowired(required = false)
    private LecturerQuotaStrategy lecturerStrategy;

    @Autowired(required = false)
    private AdminQuotaStrategy adminStrategy;

    @Autowired(required = false)
    private FacilityManagerQuotaStrategy facilityManagerStrategy;

    @PostConstruct
    public void registerStrategies() {
        log.info("Bootstrapping quota policy strategies");

        if (studentStrategy != null) {
            rolePolicyResolver.registerStrategy("USER", studentStrategy);
            log.info("Registered USER quota strategy");
        } else {
            log.warn("StudentQuotaStrategy not yet implemented (T053)");
        }

        if (lecturerStrategy != null) {
            rolePolicyResolver.registerStrategy("LECTURER", lecturerStrategy);
            log.info("Registered LECTURER quota strategy");
        } else {
            log.warn("LecturerQuotaStrategy not yet implemented (T053)");
        }

        if (adminStrategy != null) {
            rolePolicyResolver.registerStrategy("ADMIN", adminStrategy);
            log.info("Registered ADMIN quota strategy");
        } else {
            log.warn("AdminQuotaStrategy not yet implemented (T053)");
        }

        if (facilityManagerStrategy != null) {
            rolePolicyResolver.registerStrategy("FACILITY_MANAGER", facilityManagerStrategy);
            log.info("Registered FACILITY_MANAGER quota strategy");
        } else {
            log.warn("FacilityManagerQuotaStrategy not yet implemented");
        }

        log.info("Quota policy bootstrap complete. Registered roles: {}", 
                 rolePolicyResolver.getRegisteredRoles());
    }
}
