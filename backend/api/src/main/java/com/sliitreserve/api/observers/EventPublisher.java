package com.sliitreserve.api.observers;

/**
 * Event Publisher Interface for Observer Pattern.
 * 
 * Defines contract for publishing domain events to registered observers.
 * Provides decoupling between event sources (services) and event consumers (observers).
 * 
 * Design:
 * - Single responsibility: Publish events, manage subscriptions
 * - Async execution: Uses Spring's TaskExecutor or @Async for non-blocking dispatch
 * - Error isolation: Failures in one observer don't affect others
 * - Event deduplication: Can use eventId to prevent duplicate processing
 * 
 * Implementation Approach (suggested):
 * - Use Spring ApplicationEventPublisher or custom EventBus
 * - Register observers via dependency injection or @EventListener
 * - Dispatch events to matching observers based on EventFilter logic
 * - Log published events for audit trail and replay
 * 
 * Integration Points:
 * - BookingService publishes BOOKING_REQUEST_SUBMITTED, BOOKING_APPROVED, etc.
 * - TicketService publishes TICKET_CREATED, TICKET_ESCALATED, etc.
 * - ApprovalWorkflow publishes approval workflow state changes
 * - CheckInService publishes CHECK_IN_SUCCESS, NO_SHOW_RECORDED
 * - SuspensionService publishes USER_SUSPENDED, APPEAL_SUBMITTED
 * - SlaScheduler publishes SLA_DEADLINE_APPROACHING, SLA_DEADLINE_BREACHED
 * 
 * Example Usage (in a service):
 * ```java
 * @Service
 * public class BookingService {
 *     @Autowired
 *     private EventPublisher eventPublisher;
 * 
 *     public void submitBookingRequest(BookingRequest request) {
 *         // ... create booking ...
 *         
 *         eventPublisher.publish(EventEnvelope.builder()
 *             .eventId(UUID.randomUUID().toString())
 *             .eventType("BOOKING_REQUEST_SUBMITTED")
 *             .severity(EventSeverity.STANDARD)
 *             .affectedUserId(request.getUserId())
 *             .title("Booking Request Submitted")
 *             .description("Your booking request has been received")
 *             .source("BookingService")
 *             .entityReference("booking:" + booking.getId())
 *             .actionUrl("/bookings/" + booking.getId())
 *             .actionLabel("View Booking")
 *             .metadata(Map.of(
 *                 "facilityId", facility.getId(),
 *                 "startTime", booking.getStartTime()
 *             ))
 *             .build());
 *     }
 * }
 * ```
 */
public interface EventPublisher {

    /**
     * Publish domain event to all registered observers.
     * 
     * Asynchronously dispatches event to observers that declare interest via canHandle().
     * Observers are notified in order (undefined if multiple match).
     * Failure in one observer should not prevent notification of others.
     * 
     * @param event The domain event to publish
     * @throws IllegalArgumentException if event is null or incomplete
     */
    void publish(EventEnvelope event);

    /**
     * Register observer to receive events.
     * 
     * Observers are typically discovered via Spring component scanning,
     * but explicit registration is available for dynamic observers.
     * 
     * @param observer The observer to register
     */
    void subscribe(Observer observer);

    /**
     * Unregister observer from receiving events.
     * 
     * Removes observer from active subscriptions.
     * Useful for cleanup or disabling observers at runtime.
     * 
     * @param observer The observer to unregister
     */
    void unsubscribe(Observer observer);
}
