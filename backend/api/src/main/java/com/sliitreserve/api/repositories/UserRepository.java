package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.auth.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity persistence.
 *
 * <p>Handles CRUD operations and custom queries for User entities.
 * Supports user lookup by Google OAuth subject, email, and other identity fields.
 *
 * @see User for the user entity
 */
@Repository
public interface UserRepository extends BaseRepository<User, UUID> {

    /**
     * Find user by Google OAuth 2.0 Subject identifier.
     *
     * <p>The Google Subject (sub) is a unique, immutable identifier assigned by Google
     * for each user account. This is the primary key for OAuth identity mapping.
     *
     * @param googleSubject Google's unique user identifier
     * @return User if found, empty Optional otherwise
     */
    Optional<User> findByGoogleSubject(String googleSubject);

    /**
     * Find user by institutional email address.
     *
     * <p>Email is unique per user. Used for profile lookups and email-based
     * verification in certain workflows.
     *
     * @param email Institutional email address
     * @return User if found, empty Optional otherwise
     */
    Optional<User> findByEmail(String email);
}
