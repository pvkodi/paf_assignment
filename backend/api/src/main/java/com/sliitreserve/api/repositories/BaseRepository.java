package com.sliitreserve.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * Base repository interface for all domain entities.
 * Provides common CRUD operations and batch operations.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Check if an entity exists by its primary key.
     *
     * @param id Entity ID
     * @return true if entity exists, false otherwise
     */
    boolean existsById(ID id);

    /**
     * Find an entity by ID or throw NoSuchElementException.
     *
     * @param id Entity ID
     * @return Entity if found
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    default T findByIdOrThrow(ID id) {
        return this.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Entity not found with id: " + id
                ));
    }
}
