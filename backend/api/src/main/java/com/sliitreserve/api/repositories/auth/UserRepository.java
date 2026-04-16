package com.sliitreserve.api.repositories.auth;

import com.sliitreserve.api.entities.auth.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends com.sliitreserve.api.repositories.BaseRepository<User, UUID> {

    Optional<User> findByGoogleSubject(String googleSubject);

    Optional<User> findByEmail(String email);

    /**
     * Find all users whose suspension time has passed (in the past).
     * 
     * Used by SuspensionCleanupScheduler to clean up expired suspensions.
     * Returns users with suspendedUntil set and before the given cutoff time.
     * 
     * @param cutoffDateTime Current time (suspensions before this are expired)
     * @return List of users with expired suspensions
     */
    @Query("SELECT u FROM User u WHERE u.suspendedUntil IS NOT NULL AND u.suspendedUntil < :cutoff")
    List<User> findBySuspendedUntilNotNullAndSuspendedUntilBefore(@Param("cutoff") LocalDateTime cutoffDateTime);
}
