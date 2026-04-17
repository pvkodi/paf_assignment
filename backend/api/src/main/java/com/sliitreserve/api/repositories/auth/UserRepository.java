package com.sliitreserve.api.repositories.auth;

import com.sliitreserve.api.entities.auth.Role;
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

    /**
     * Find all active users with a specific role.
     * Queries the user_roles element collection to find users with the given role.
     *
     * @param role The role to search for (e.g., TECHNICIAN, FACILITY_MANAGER, ADMIN)
     * @return List of active users with the specified role
     */
    @Query("SELECT u FROM User u WHERE :role MEMBER OF u.roles AND u.active = true")
    List<User> findByRoleAndActiveTrue(@Param("role") Role role);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :emailPattern, '%')) " +
           "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :displayNamePattern, '%')) " +
           "ORDER BY u.displayName ASC, u.email ASC")
    List<User> searchByEmailOrDisplayName(@Param("emailPattern") String emailPattern, 
                                          @Param("displayNamePattern") String displayNamePattern);
}
