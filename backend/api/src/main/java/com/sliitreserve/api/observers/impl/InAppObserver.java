package com.sliitreserve.api.observers.impl;

import com.sliitreserve.api.entities.notification.Notification;
import com.sliitreserve.api.entities.notification.NotificationChannel;
import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventSeverity;
import com.sliitreserve.api.observer.Observer;
import com.sliitreserve.api.repositories.NotificationRepository;
import com.sliitreserve.api.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * In-App Notification Observer Implementation
 *
 * Purpose: Handles delivery of notifications to user in-app notification inbox.
 *
 * Routing Rules (from FR-051, FR-052, Research Decision 8):
 * - Handles HIGH severity events: SLA escalations, suspensions, security alerts
 * - Handles STANDARD severity events: Booking approvals, ticket assignments, reminders
 * - Routes to: In-app notification inbox for affected user
 *
 * Design:
 * - Non-blocking: Marked with @Async to prevent blocking event publisher
 * - Idempotent: Safe if called multiple times with same event (uses eventId for deduplication)
 * - Exception-safe: Catches and logs exceptions without re-throwing
 * - Discoverable: Marked with @Component for Spring classpath scanning
 *
 * Implementation Approach:
 * - canHandle(): Always returns true (InApp accepts all severity levels)
 * - handleEvent(): Creates NotificationRecord in database for affected user
 * - Logs all operations for audit trail
 * - Handles user lookup failure gracefully (logs warning, continues)
 *
 * Integration:
 * - Subscribed automatically to EventPublisher via @Component discovery
 * - Receives events from NotificationService publishers (BookingService, TicketService, etc.)
 * - Persists to notification table for inbox queries
 * - Optional: Can include pagination filtering for large notification lists
 *
 * Examples of Events Handled:
 * - BOOKING_APPROVED (STANDARD severity)
 * - CHECK_IN_SUCCESS (STANDARD severity)
 * - TICKET_ASSIGNED (STANDARD severity)
 * - NO_SHOW_RECORDED (STANDARD severity)
 * - SLA_DEADLINE_BREACHED (HIGH severity)
 * - USER_SUSPENDED (HIGH severity)
 * - TICKET_ESCALATED (HIGH severity)
 */
@Component
@Async
@Slf4j
public class InAppObserver implements Observer {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Handle domain event for in-app notification delivery.
     *
     * Creates a Notification record in the user's inbox for the affected user.
     * Idempotent via eventId deduplication at database level.
     * Non-blocking via @Async.
     * Exception-safe: logs and continues on failure.
     *
     * @param event The domain event to handle (must not be null)
     */
    @Override
    public void handleEvent(EventEnvelope event) {
        if (event == null || event.getAffectedUserId() == null) {
            log.warn("InAppObserver: Cannot handle event without affected user ID. Event: {}", 
                    event != null ? event.getEventType() : "null");
            return;
        }

        try {
            log.debug("InAppObserver processing event type: {}, severity: {}, affected user: {}",
                    event.getEventType(),
                    event.getSeverity(),
                    event.getAffectedUserId());

            // Check if notification already exists (idempotency)
            if (event.getEventId() != null) {
                var existing = notificationRepository.findByEventId(event.getEventId());
                if (existing.isPresent()) {
                    log.debug("InAppObserver: Notification already exists for event ID {}, skipping", event.getEventId());
                    return;
                }
            }

            // Look up recipient user
            var recipient = userRepository.findById(UUID.fromString(event.getAffectedUserId()));
            if (recipient.isEmpty()) {
                log.warn("InAppObserver: Recipient user not found: {}", event.getAffectedUserId());
                return;
            }

            // Create notification record
            Notification notification = Notification.builder()
                .recipientUser(recipient.get())
                .eventType(event.getEventType())
                .severity(event.getSeverity())
                .title(event.getTitle())
                .message(event.getDescription())
                .channels(Set.of(NotificationChannel.IN_APP))
                .eventId(event.getEventId())
                .entityReference(event.getEntityReference())
                .actionUrl(event.getActionUrl())
                .actionLabel(event.getActionLabel())
                .deliveredAt(LocalDateTime.now())
                .build();

            notificationRepository.save(notification);

            log.info("InAppObserver: Created in-app notification {} for user {} - event type: {}",
                    notification.getId(),
                    event.getAffectedUserId(),
                    event.getEventType());

        } catch (Exception e) {
            // Log but don't throw - maintain observer isolation
            log.error("InAppObserver failed to create notification for user {} - event type: {}: {}",
                    event.getAffectedUserId(),
                    event.getEventType(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Check if this observer is interested in the given event.
     *
     * InAppObserver accepts all severity levels (HIGH and STANDARD).
     * Returns true if event has required fields (eventType, affectedUserId).
     *
     * @param event The event to evaluate for handling
     * @return true if this observer should handle the event (always true for valid events)
     */
    @Override
    public boolean canHandle(EventEnvelope event) {
        if (event == null) {
            return false;
        }

        // InAppObserver requires:
        // 1. Event type (for logging and routing)
        // 2. Affected user ID (for inbox delivery)
        // 3. Title and description (for display)
        boolean canHandle = event.getEventType() != null
                && !event.getEventType().isBlank()
                && event.getAffectedUserId() != null
                && event.getTitle() != null
                && !event.getTitle().isBlank();

        if (!canHandle) {
            log.trace("InAppObserver: Insufficient fields in event - {}", event.getEventType());
        }

        return canHandle;
    }
}
