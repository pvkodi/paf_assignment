package com.sliitreserve.api.observer;

/**
 * Event Severity Levels.
 * 
 * Determines notification channel routing based on event priority.
 * 
 * From Research Decision 8: Notification Architecture
 * - HIGH: SLA escalations, suspensions -> email + in-app notifications
 * - STANDARD: booking approvals, reminders -> in-app notifications only
 * 
 * From Specification (FR-051, FR-052):
 * - HIGH: Events such as escalations or suspensions trigger both email and in-app
 * - STANDARD: Events such as booking approval and reminders trigger in-app only
 * 
 * Usage:
 * - NotificationService checks severity for observer routing
 * - InAppObserver handles both HIGH and STANDARD
 * - EmailObserver handles only HIGH severity
 * - Monitoring/alerts can filter by severity
 */
public enum EventSeverity {
    /**
     * High-priority events requiring immediate attention.
     * Routes to: InAppObserver + EmailObserver
     * 
     * Examples:
     * - SLA deadline breached (critical/high priority tickets)
     * - User suspension notification
     * - Access denied / security alert
     * - Escalation triggered
     */
    HIGH,

    /**
     * Standard-priority events for routine information.
     * Routes to: InAppObserver only
     * 
     * Examples:
     * - Booking approved/rejected
     * - Ticket assigned
     * - Comment posted
     * - Reminder notifications
     */
    STANDARD
}
