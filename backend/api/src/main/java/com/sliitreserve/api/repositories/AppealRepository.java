package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.auth.SuspensionAppeal;
import com.sliitreserve.api.entities.auth.SuspensionAppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SuspensionAppeal entities.
 *
 * <p>Provides database access for appeal CRUD operations and queries for appeal management.
 * Supports filtering by user, status, and timeframe.
 *
 * <p><b>Integration Points</b>:
 * <ul>
 *   <li>AppealService: Uses all repository methods for appeal lifecycle
 *   <li>AppealController: Calls repository indirectly via service
 * </ul>
 *
 * @see SuspensionAppeal for the entity being queried
 * @see AppealService for service-layer logic
 */
@Repository
public interface AppealRepository extends JpaRepository<SuspensionAppeal, UUID> {

  /**
   * Find all appeals submitted by a specific user, ordered by most recent first.
   *
   * @param userId UUID of the user
   * @return List of appeals from this user (oldest to newest)
   */
  List<SuspensionAppeal> findByUserIdOrderByCreatedAtDesc(UUID userId);

  /**
   * Find all pending/submitted appeals (awaiting admin review), ordered by oldest first.
   *
   * <p>Used by admin to retrieve the review queue.
   *
   * @return List of appeals with status SUBMITTED
   */
  List<SuspensionAppeal> findByStatusOrderByCreatedAtAsc(SuspensionAppealStatus status);

  /**
   * Find all pending appeals with pagination.
   *
   * <p>Supports paginated admin review queue.
   *
   * @param status Appeal status to filter by
   * @param pageable Pagination info
   * @return Paginated list of appeals
   */
  Page<SuspensionAppeal> findByStatus(SuspensionAppealStatus status, Pageable pageable);

  /**
   * Find the most recent pending appeal for a user.
   *
   * <p>Used to prevent duplicate appeal submissions (user shouldn't submit multiple pending appeals).
   *
   * @param userId UUID of the user
   * @return Optional containing the most recent pending appeal, or empty if none
   */
  Optional<SuspensionAppeal> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, SuspensionAppealStatus status);

  /**
   * Count pending appeals for a user.
   *
   * <p>Used to check if user has active pending appeal (should not submit another).
   *
   * @param userId UUID of the user
   * @return Count of pending appeals for this user
   */
  long countByUserIdAndStatus(UUID userId, SuspensionAppealStatus status);
}
