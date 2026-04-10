package com.sliitreserve.api.services.notification;

import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventPublisher;
import com.sliitreserve.api.observer.EventSeverity;
import com.sliitreserve.api.observer.Observer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Notification Service Implementation - Observer Pattern Event Publisher
 *
 * Purpose: Coordinates publication of domain events to registered observers
 * with severity-based routing, template management, and audit logging.
 *
 * Responsible for:
 * - Event validation and enrichment
 * - Severity-based routing to appropriate observers
 * - Observer management and discovery
 * - Event audit logging for compliance
 * - Template management for notifications
 * - Ensuring observer isolation on failures
 * - Async event dispatch for non-blocking performance
 *
 * Event Routing Rules (from FR-051, FR-052, Research Decision 8):
 * - HIGH severity: Routes to all observers (InApp + Email)
 *   Examples: SLA breaches, user suspensions, critical alerts
 * - STANDARD severity: Routes to InApp-only observers
 *   Examples: Booking approvals, reminders, routine updates
 *
 * Example Usage (in a service):
 * ```java
 * @Service
 * public class BookingService {
 *     @Autowired
 *     private NotificationService notificationService;
 *
 *     public void approveBooking(Booking booking) {
 *         // ... approval logic ...
 *         
 *         notificationService.publish(EventEnvelope.builder()
 *             .eventId(UUID.randomUUID().toString())
 *             .eventType("BOOKING_APPROVED")
 *             .severity(EventSeverity.STANDARD)
 *             .affectedUserId(booking.getRequestedByUserId())
 *             .title("Booking Approved")
 *             .description("Your booking has been approved")
 *             .source("BookingService")
 *             .entityReference("booking:" + booking.getId())
 *             .actionUrl("/bookings/" + booking.getId())
 *             .actionLabel("View Booking")
 *             .build());
 *     }
 * }
 * ```
 *
 * Design Patterns:
 * - Observer Pattern: Decouples event sources from event handlers
 * - Template Method: Email/notification template generation per event type
 * - Chain of Responsibility: Multiple observers proces events independently
 * - Event Sourcing: Audit log tracks all published events
 */
@Service
@Slf4j
public class NotificationServiceImpl implements EventPublisher {

    private final Set<Observer> observers = new CopyOnWriteArraySet<>();
    private final EmailTemplateFactory emailTemplateFactory;

    @Autowired(required = false)
    public NotificationServiceImpl(EmailTemplateFactory emailTemplateFactory) {
        this.emailTemplateFactory = emailTemplateFactory;
        log.info("NotificationService initialized with optional EmailTemplateFactory");
    }

    public NotificationServiceImpl() {
        this(null);
    }

    /**
     * Publish domain event to all registered observers.
     *
     * Step 1: Validate event completeness and required fields
     * Step 2: Enrich event with timestamp and processing metadata
     * Step 3: Log event to audit trail
     * Step 4: Route to observers based on severity and interest (canHandle)
     * Step 5: Handle failures in observer dispatch with isolation
     *
     * Dispatch is non-blocking (completes quickly) with actual notification
     * delivery happening asynchronously in observer implementations.
     *
     * @param event The domain event to publish
     * @throws IllegalArgumentException if event is null or missing required fields
     */
    @Override
    public void publish(EventEnvelope event) {
        // Step 1: Validate event
        validateEvent(event);

        // Step 2: Enrich event with routing metadata
        enrichEvent(event);

        // Step 3: Log to audit trail
        auditLogEvent(event);

        // Step 4: Route to observers
        dispatchToObservers(event);

        log.info("Event published successfully - type: {}, severity: {}, affected user: {}",
                event.getEventType(),
                event.getSeverity(),
                event.getAffectedUserId());
    }

    /**
     * Validate event completeness and required fields.
     *
     * Required fields:
     * - eventId: Unique event identifier (UUID format)
     * - eventType: Event classification (e.g., BOOKING_APPROVED, SLA_DEADLINE_BREACHED)
     * - severity: HIGH or STANDARD (determines routing)
     * - title: Human-readable event title for notifications
     * - description: Detailed event description
     *
     * Optional fields:
     * - affectedUserId: User affected by this event (nullable for system-wide events)
     * - entityReference: Reference to related entity (format: type:id)
     * - actionUrl: Deep link for notification CTAs
     *
     * @param event The event to validate
     * @throws IllegalArgumentException if required fields are missing/empty
     */
    private void validateEvent(EventEnvelope event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getEventType() == null || event.getEventType().isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }

        if (event.getSeverity() == null) {
            throw new IllegalArgumentException("Event severity is required (HIGH or STANDARD)");
        }

        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new IllegalArgumentException("Event title is required");
        }

        if (event.getDescription() == null || event.getDescription().isBlank()) {
            throw new IllegalArgumentException("Event description is required");
        }

        if (event.getEventId() == null || event.getEventId().isBlank()) {
            // Auto-generate if missing
            event.setEventId(UUID.randomUUID().toString());
        }

        log.trace("Event validation passed - type: {}, id: {}",
                event.getEventType(),
                event.getEventId());
    }

    /**
     * Enrich event with routing and processing metadata.
     *
     * Adds/updates:
     * - occurrenceTime: When the event occurred (if not already set)
     * - publishedAt: When the event was published to the system
     * - Tags: Categories for filtering and monitoring
     *
     * @param event The event to enrich
     */
    private void enrichEvent(EventEnvelope event) {
        if (event.getOccurrenceTime() == null) {
            event.setOccurrenceTime(ZonedDateTime.now());
        }

        event.setPublishedAt(ZonedDateTime.now());

        // Add routing tags for severity-based operations
        if (event.getTags() == null) {
            event.setTags(new HashSet<>());
        }
        event.getTags().add(event.getSeverity().name().toLowerCase());
        event.getTags().add("notification:" + event.getEventType().toLowerCase());

        log.trace("Event enriched - tags: {}", event.getTags());
    }

    /**
     * Log event to audit trail for compliance and troubleshooting.
     *
     * Captures:
     * - Event ID, type, and severity for tracing
     * - Affected user and secondary user (if any)
     * - Event source module/service
     * - Timestamp for chronological ordering
     * - Tags and metadata for filtering
     *
     * @param event The event to audit log
     */
    private void auditLogEvent(EventEnvelope event) {
        StringBuilder auditLog = new StringBuilder()
                .append("EVENT_LOGGED:")
                .append(" id=").append(event.getEventId())
                .append(" type=").append(event.getEventType())
                .append(" severity=").append(event.getSeverity())
                .append(" user=").append(event.getAffectedUserId())
                .append(" source=").append(event.getSource());

        if (event.getSecondaryUserId() != null) {
            auditLog.append(" secondary_user=").append(event.getSecondaryUserId());
        }

        log.info(auditLog.toString());
    }

    /**
     * Dispatch event to all registered observers that can handle it.
     *
     * Algorithm:
     * 1. Iterate through all registered observers
     * 2. For each observer, call canHandle(event) to check interest
     * 3. If interested, dispatch event asynchronously to observer.handleEvent()
     * 4. Catch exceptions in observer dispatch to ensure isolation
     * 5. Continue with remaining observers even if one fails
     *
     * Observers are called asynchronously (via @Async in observer impl),
     * so this method returns quickly without waiting for notification delivery.
     *
     * @param event The event to dispatch
     */
    private void dispatchToObservers(EventEnvelope event) {
        if (observers.isEmpty()) {
            log.debug("No observers registered - event will not be dispatched");
            return;
        }

        int dispatchCount = 0;

        for (Observer observer : observers) {
            try {
                if (observer.canHandle(event)) {
                    log.debug("Dispatching event {} to observer {}",
                            event.getEventType(),
                            observer.getClass().getSimpleName());

                    observer.handleEvent(event);
                    dispatchCount++;
                } else {
                    log.trace("Observer {} declined event {}",
                            observer.getClass().getSimpleName(),
                            event.getEventType());
                }
            } catch (Exception e) {
                // Observer isolation: log failure but continue with others
                log.error("Observer {} failed to handle event {} - continuing with others: {}",
                        observer.getClass().getSimpleName(),
                        event.getEventType(),
                        e.getMessage(),
                        e);
            }
        }

        log.debug("Event {} dispatched to {} observers out of {}",
                event.getEventType(),
                dispatchCount,
                observers.size());
    }

    /**
     * Register observer to receive events.
     *
     * Observers are typically discovered via Spring component scanning (@Component)
     * and auto-registered. This method allows explicit registration for dynamic
     * observers created at runtime.
     *
     * Uses CopyOnWriteArraySet to allow safe iteration during concurrent modifications.
     *
     * @param observer The observer to register
     */
    @Override
    public void subscribe(Observer observer) {
        if (observer != null) {
            boolean added = observers.add(observer);
            if (added) {
                log.info("Observer subscribed: {} (total: {})",
                        observer.getClass().getSimpleName(),
                        observers.size());
            } else {
                log.debug("Observer already subscribed: {}",
                        observer.getClass().getSimpleName());
            }
        } else {
            log.warn("Cannot subscribe null observer");
        }
    }

    /**
     * Unregister observer from receiving events.
     *
     * Removes observer from active subscriptions.
     * Useful for cleanup or temporarily disabling observers at runtime.
     *
     * @param observer The observer to unregister
     */
    @Override
    public void unsubscribe(Observer observer) {
        if (observer != null) {
            boolean removed = observers.remove(observer);
            if (removed) {
                log.info("Observer unsubscribed: {} (total: {})",
                        observer.getClass().getSimpleName(),
                        observers.size());
            } else {
                log.debug("Observer was not subscribed: {}",
                        observer.getClass().getSimpleName());
            }
        } else {
            log.warn("Cannot unsubscribe null observer");
        }
    }

    /**
     * Get the current number of registered observers.
     *
     * Used for testing, monitoring, and diagnostics.
     *
     * @return count of active observers
     */
    public int getObserverCount() {
        return observers.size();
    }

    /**
     * Get email template for a specific event type.
     *
     * Allows services to access email templates for custom rendering or preview.
     * Uses EmailTemplateFactory if available (T079 - SMTP configuration).
     *
     * @param eventType The event type (e.g., SLA_DEADLINE_BREACHED)
     * @return Email template subject line, or generic subject if not found
     */
    public String getEmailSubjectForEventType(String eventType) {
        if (emailTemplateFactory != null) {
            return emailTemplateFactory.getSubjectForEventType(eventType);
        }

        // Fallback templates (T079 will provide customizable versions)
        return switch (eventType) {
            case "SLA_DEADLINE_APPROACHING" -> "⚠️ Maintenance Ticket: SLA Deadline Approaching";
            case "SLA_DEADLINE_BREACHED" -> "🚨 URGENT: Maintenance Ticket SLA Breached";
            case "TICKET_ESCALATED" -> "🚨 Maintenance Ticket Escalated";
            case "USER_SUSPENDED" -> "⛔ Account Suspended";
            case "TICKET_CREATED" -> "🎟️ New Maintenance Ticket Created";
            case "APPEAL_REJECTED" -> "⛔ Suspension Appeal Rejected";
            case "APPEAL_APPROVED" -> "✅ Suspension Appeal Approved";
            default -> "Campus Operations Notification";
        };
    }

    /**
     * Get email template body for a specific event type.
     *
     * Allows services to access email templates for custom rendering.
     * Uses EmailTemplateFactory if available, otherwise returns generic template.
     *
     * @param eventType The event type
     * @param variables Event-specific template variables
     * @return Email body content (HTML or plain text)
     */
    public String getEmailBodyForEventType(String eventType, Map<String, Object> variables) {
        if (emailTemplateFactory != null) {
            return emailTemplateFactory.getBodyForEventType(eventType, variables);
        }

        // Fallback generic template (T079 will provide extensive templates)
        return buildGenericEmailTemplate(eventType, variables);
    }

    /**
     * Build generic email template for events without specific templates.
     *
     * This is a fallback when EmailTemplateFactory is not available.
     * T079 will provide comprehensive, branded email templates.
     *
     * @param eventType The event type
     * @param variables Template variables
     * @return HTML email body
     */
    private String buildGenericEmailTemplate(String eventType, Map<String, Object> variables) {
        String recipientName = (String) variables.getOrDefault("recipientName", "User");
        String eventTitle = (String) variables.getOrDefault("eventTitle", "Campus Notification");
        String eventDescription = (String) variables.getOrDefault("eventDescription", "You have a new notification");
        String actionUrl = (String) variables.getOrDefault("actionUrl", "/dashboard");
        String actionLabel = (String) variables.getOrDefault("actionLabel", "View Details");

        return String.format(
                "<html><body>"
                        + "<p>Dear %s,</p>"
                        + "<h2>%s</h2>"
                        + "<p>%s</p>"
                        + "<p><a href=\"%s\" style=\"padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px;\">%s</a></p>"
                        + "<hr>"
                        + "<p><small>This is an automated notification from Campus Operations Hub.</small></p>"
                        + "</body></html>",
                recipientName,
                eventTitle,
                eventDescription,
                actionUrl,
                actionLabel
        );
    }
}

