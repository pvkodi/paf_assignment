package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Notification entity.
 *
 * Queries:
 * - Find all notifications for user (paginated, sorted by date)
 * - Find unread notifications for user
 * - Find by eventId (idempotency check)
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find all notifications for a user, paginated and sorted by creation date (newest first).
     *
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of notifications
     */
    Page<Notification> findByRecipientUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all unread notifications for a user.
     *
     * @param userId The user ID
     * @return List of unread notifications sorted by date
     */
    @Query("SELECT n FROM Notification n WHERE n.recipientUser.id = :userId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") UUID userId);

    /**
     * Find notification by eventId for idempotency check.
     *
     * @param eventId The event ID
     * @return Optional notification if found
     */
    Optional<Notification> findByEventId(String eventId);

    /**
     * Count unread notifications for a user.
     *
     * @param userId The user ID
     * @return Count of unread notifications
     */
    long countByRecipientUser_IdAndReadAtIsNull(UUID userId);
}
