package com.sliitreserve.api.observer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Event Envelope for Observer Pattern.
 * 
 * Container for domain events published throughout the system.
 * Provides context for routing to appropriate observers based on severity
 * and event type.
 * 
 * Event Types (from specification):
 * - Booking Events: REQUEST_SUBMITTED, REQUEST_APPROVED, REQUEST_REJECTED, RECURRING_HOLIDAY_SKIPPED
 * - Check-in Events: CHECK_IN_SUCCESS, NO_SHOW_RECORDED
 * - Suspension Events: USER_SUSPENDED, APPEAL_SUBMITTED, APPEAL_APPROVED, APPEAL_REJECTED
 * - Ticket Events: TICKET_CREATED, TICKET_ASSIGNED, TICKET_ESCALATED, TICKET_CLOSED
 * - SLA Events: SLA_DEADLINE_APPROACHING, SLA_DEADLINE_BREACHED
 * 
 * Severity Levels (from Research Decision 8):
 * - HIGH: SLA escalations, suspensions -> email + in-app notifications (FR-051)
 * - STANDARD: booking approvals, reminders -> in-app only (FR-052)
 * 
 * Design:
 * - Immutable after construction via builder
 * - Supports arbitrary event metadata via Map<String, Object>
 * - Contains affected user ID for routing to inbox
 * - Carries event timestamp for audit and replay capabilities
 * - Event source identifies originating module/service
 * 
 * Integration Points:
 * - EventBus: Publishes events to registered observers
 * - InAppObserver: Creates in-app notification records
 * - EmailObserver: Dispatches email notifications
 * - NotificationService: Routes events based on severity
 * - EventLog: Persists events for audit trail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique event identifier for tracing and deduplication.
     * Format: UUID
     */
    private String eventId;

    /**
     * Event type classification for routing and filtering.
     * Examples: BOOKING_REQUEST_SUBMITTED, TICKET_ESCALATED, USER_SUSPENDED
     */
    private String eventType;

    /**
     * Event severity determining notification channels.
     * HIGH: Both email and in-app notifications
     * STANDARD: In-app notifications only
     */
    private EventSeverity severity;

    /**
     * User ID affected by this event.
     * Used for notification inbox routing and access control.
     * Can be null for system-wide events.
     */
    private Long affectedUserId;

    /**
     * Optional secondary user ID (admin, approver, technician).
     * Used for escalation notifications and assignment alerts.
     */
    private Long secondaryUserId;

    /**
     * Human-readable event title for notifications.
     * Examples: "Booking Approved", "SLA Deadline Approaching", "Ticket Assigned"
     */
    private String title;

    /**
     * Detailed event description for in-app and email notifications.
     * Can contain HTML for email rendering.
     */
    private String description;

    /**
     * Source module/service that published this event.
     * Examples: "BookingService", "TicketService", "ApprovalWorkflow"
     */
    private String source;

    /**
     * Timestamp when event occurred (in campus timezone).
     * Set by event publisher.
     */
    private ZonedDateTime occurrenceTime;

    /**
     * Timestamp when event was published to system.
     * Set by EventBus/EventPublisher.
     */
    private ZonedDateTime publishedAt;

    /**
     * Optional reference to related entity (booking ID, ticket ID, etc.).
     * Format: "{entityType}:{entityId}"
     * Examples: "booking:12345", "ticket:67890"
     */
    private String entityReference;

    /**
     * Optional URL for deep linking in notifications.
     * Examples: "/bookings/12345", "/tickets/67890"
     */
    private String actionUrl;

    /**
     * Optional action label for notification CTA.
     * Examples: "View Booking", "Review Request", "Assign Now"
     */
    private String actionLabel;

    /**
     * Event metadata for extensibility.
     * Contains event-specific data without changing envelope structure.
     * Examples:
     * - BOOKING_REQUEST_SUBMITTED: {"facilityId": 1, "startTime": "2024-04-10T09:00:00", "bookedFor": "user_email"}
     * - TICKET_ESCALATED: {"ticketId": 42, "escalationLevel": 2, "newAssignee": "tech_name"}
     * - USER_SUSPENDED: {"suspensionReason": "3 no-shows", "duration": "7 days"}
     */
    private Map<String, Object> metadata;

    /**
     * Tags for filtering and categorization.
     * Examples: ["booking", "approval", "urgent"], ["ticket", "escalation"]
     */
    private java.util.Set<String> tags;
}
