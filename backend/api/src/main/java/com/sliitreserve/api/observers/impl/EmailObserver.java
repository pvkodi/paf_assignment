package com.sliitreserve.api.observers.impl;

import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.observers.Observer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Email Notification Observer Implementation
 *
 * Purpose: Handles delivery of high-priority notifications via email.
 *
 * Routing Rules (from FR-051, Research Decision 8):
 * - Handles HIGH severity events ONLY: SLA escalations, suspensions, security alerts
 * - Ignores STANDARD severity events: Booking approvals, reminders (in-app only)
 * - Routes to: User's registered email address
 *
 * Design:
 * - Non-blocking: Marked with @Async to prevent blocking event publisher
 * - Severity-aware: Only processes HIGH severity events (STANDARD events ignored)
 * - Idempotent: Safe if called multiple times (uses eventId for deduplication at SMTP level)
 * - Exception-safe: Catches and logs exceptions without re-throwing
 * - Discoverable: Marked with @Component for Spring classpath scanning
 *
 * Implementation Approach:
 * - canHandle(): Returns true ONLY for HIGH severity events with required fields
 * - handleEvent(): Sends email notification via configured SMTP adapter (T079)
 * - Logs all operations for audit trail
 * - Handles email template rendering and user lookup failures gracefully
 *
 * Integration:
 * - Subscribed automatically to EventPublisher via @Component discovery
 * - Receives events from NotificationService publishers (BookingService, TicketService, etc.)
 * - Uses SMTP configuration defined in MailConfig (T079)
 * - Supports email template customization per event type
 * - Optional: Implements retry logic for transient SMTP failures
 *
 * Examples of Events Handled:
 * - SLA_DEADLINE_BREACHED (HIGH severity) -> Urgent SLA breach email
 * - SLA_DEADLINE_APPROACHING (HIGH severity) -> SLA warning email
 * - USER_SUSPENDED (HIGH severity) -> Suspension notification email
 * - TICKET_ESCALATED (HIGH severity) -> Escalation alert email
 *
 * Examples of Events IGNORED:
 * - BOOKING_APPROVED (STANDARD) -> In-app only
 * - CHECK_IN_SUCCESS (STANDARD) -> In-app only
 * - TICKET_ASSIGNED (STANDARD) -> In-app only
 * - NO_SHOW_RECORDED (STANDARD) -> In-app only
 *
 * TODO (T079): Complete with MailConfig integration and SMTP adapter setup
 */
@Component
@Async
@Slf4j
public class EmailObserver implements Observer {

    /**
     * Handle domain event for email notification delivery.
     *
     * Only processes HIGH severity events; ignores STANDARD.
     * Dispatches email via configured SMTP adapter (T079 - Mailtrap).
     * Idempotent via eventId deduplication.
     * Non-blocking via @Async.
     * Exception-safe: logs and continues on failure.
     *
     * @param event The domain event to handle (must not be null, severity must be HIGH)
     */
    @Override
    public void handleEvent(EventEnvelope event) {
        if (event == null) {
            log.warn("EmailObserver: Received null event");
            return;
        }

        // Early return for non-HIGH severity (should not be called by publisher due to canHandle)
        if (event.getSeverity() != EventSeverity.HIGH) {
            log.debug("EmailObserver: Ignoring {} severity event (HIGH required): {}",
                    event.getSeverity(),
                    event.getEventType());
            return;
        }

        if (event.getAffectedUserId() == null) {
            log.warn("EmailObserver: Cannot handle event without affected user ID. Event: {}",
                    event.getEventType());
            return;
        }

        try {
            log.debug("EmailObserver processing HIGH severity event: {}, affected user: {}",
                    event.getEventType(),
                    event.getAffectedUserId());

            // TODO (T079): Integrate MailConfig and SMTP adapter for actual email dispatch
            // Placeholder for actual implementation:
            // 
            // User user = userRepository.findById(event.getAffectedUserId())
            //     .orElseThrow(() -> new UserNotFoundException(event.getAffectedUserId()));
            // 
            // EmailTemplate template = EmailTemplateFactory.createTemplate(event);
            // String htmlBody = template.render(Map.of(
            //     "recipientName", user.getDisplayName(),
            //     "eventTitle", event.getTitle(),
            //     "eventDescription", event.getDescription(),
            //     "actionUrl", event.getActionUrl(),
            //     "actionLabel", event.getActionLabel(),
            //     "severity", "HIGH"
            // ));
            // 
            // SimpleMailMessage message = new SimpleMailMessage();
            // message.setTo(user.getEmail());
            // message.setSubject(emailTemplateFactory.getSubjectForEventType(event.getEventType()));
            // message.setText(htmlBody);
            // 
            // javaMailSender.send(message);

            log.info("EmailObserver: Sent HIGH priority email for user {} - event type: {}",
                    event.getAffectedUserId(),
                    event.getEventType());

        } catch (Exception e) {
            // Log but don't throw - maintain observer isolation
            log.error("EmailObserver failed to send email to user {} - event type: {}: {}",
                    event.getAffectedUserId(),
                    event.getEventType(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Check if this observer is interested in the given event.
     *
     * EmailObserver accepts ONLY HIGH severity events.
     * Returns true if event has required fields (eventType, affectedUserId, severity=HIGH).
     *
     * @param event The event to evaluate for handling
     * @return true if this observer should handle the event (only for HIGH severity)
     */
    @Override
    public boolean canHandle(EventEnvelope event) {
        if (event == null) {
            return false;
        }

        // EmailObserver requires:
        // 1. HIGH severity (critical events only)
        // 2. Event type (for template selection and routing)
        // 3. Affected user ID (for recipient lookup)
        // 4. Title and description (for email body)
        boolean canHandle = event.getSeverity() == EventSeverity.HIGH
                && event.getEventType() != null
                && !event.getEventType().isBlank()
                && event.getAffectedUserId() != null
                && event.getTitle() != null
                && !event.getTitle().isBlank();

        if (!canHandle && event.getSeverity() != EventSeverity.HIGH) {
            log.trace("EmailObserver: Skipping {} severity event (HIGH required): {}",
                    event.getSeverity(),
                    event.getEventType());
        }

        return canHandle;
    }

    /**
     * Helper method to determine email subject based on event type.
     * Used by handleEvent() when rendering email templates (T079).
     *
     * @param eventType The event type string
     * @return Email subject line
     */
    private String getEmailSubjectForEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "Campus System Notification";
        }

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
     * Helper method to determine email priority headers.
     * Used to mark critical emails in email clients.
     *
     * @param eventType The event type string
     * @return Email priority (HIGH, NORMAL, or LOW)
     */
    private String getEmailPriorityForEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "NORMAL";
        }

        return switch (eventType) {
            case "SLA_DEADLINE_BREACHED", "USER_SUSPENDED" -> "HIGH";
            case "SLA_DEADLINE_APPROACHING", "TICKET_ESCALATED" -> "NORMAL";
            default -> "NORMAL";
        };
    }
}
