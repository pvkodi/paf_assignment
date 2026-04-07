package com.sliitreserve.api.repositories;

import org.springframework.data.repository.NoRepositoryBean;
import java.util.UUID;

/**
 * Repository placeholder for User entity.
 * Will be fully implemented in T027 (User and Role entities).
 *
 * This interface is created here to support early testing and dependency injection
 * in foundational services and tests (T023-T026).
 * 
 * @NoRepositoryBean prevents Spring Data JPA from validating this repository at startup
 * since it uses Object as a placeholder entity type (real User entity defined in T027).
 */
@NoRepositoryBean
public interface UserRepository extends BaseRepository<Object, UUID> {
    // Placeholder - will be implemented with User entity in T027
}
