package com.sliitreserve.api.repositories;

import org.springframework.data.repository.NoRepositoryBean;
import java.util.UUID;

/**
 * Repository placeholder for Booking entity.
 * Will be fully implemented in T043 (Booking entity with @Version and recurrence fields).
 *
 * This interface is created here to support early testing and dependency injection
 * in foundational services and tests.
 * 
 * @NoRepositoryBean prevents Spring Data JPA from validating this repository at startup
 * since it uses Object as a placeholder entity type (real Booking entity defined in T043).
 */
@NoRepositoryBean
public interface BookingRepository extends BaseRepository<Object, UUID> {
    // Placeholder - will be implemented with Booking entity in T043
}
