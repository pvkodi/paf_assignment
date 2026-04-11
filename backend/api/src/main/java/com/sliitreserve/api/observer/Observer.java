package com.sliitreserve.api.observer;

/**
 * Observer Interface for Event Handling.
 * 
 * Defines contract for components that react to domain events.
 * Implements the Observer pattern for loose coupling between event publishers
 * and observers.
 * 
 * Design:
 * - Single responsibility: Handle one event type or category
 * - Stateless: Observers should not maintain event state
 * - Idempotent: Handlers should be safe for duplicate event delivery
 * - Non-blocking: Typically async via Spring's @Async or TaskExecutor
 * 
 * Integration:
 * - EventBus/EventPublisher routes events to matching observers
 * - Multiple observers can subscribe to same event type
 * - Observers are Spring beans discovered via classpath scan
 * - T077, T078 implement InAppObserver and EmailObserver
 * 
 * Example Implementation (T077 - InAppObserver):
 * ```java
 * @Component
 * @Async
 * public class InAppObserver implements Observer {
 *     @Override
 *     public void handleEvent(EventEnvelope event) {
 *         NotificationRecord record = new NotificationRecord()
 *             .setUserId(event.getAffectedUserId())
 *             .setTitle(event.getTitle())
 *             .setDescription(event.getDescription());
 *         notificationRepository.save(record);
 *     }
 * }
 * ```
 * 
 * Example Implementation (T078 - EmailObserver):
 * ```java
 * @Component
 * @Async
 * public class EmailObserver implements Observer {
 *     @Override
 *     public void handleEvent(EventEnvelope event) {
 *         if (EventSeverity.HIGH != event.getSeverity()) return; // Only email HIGH
 *         
 *         User user = userRepository.findById(event.getAffectedUserId());
 *         emailService.send(EmailTemplate.forEvent(event), user.getEmail());
 *     }
 * }
 * ```
 */
public interface Observer {

    /**
     * Handle domain event notification.
     * 
     * Called by EventBus when event matching observer's subscription is published.
     * Implementation should be:
     * - Idempotent: Safe if called multiple times with same event
     * - Non-blocking: Use @Async to prevent blocking event publisher
     * - Exception-safe: Should not throw; log and continue
     * 
     * @param event The domain event to handle
     */
    void handleEvent(EventEnvelope event);

    /**
     * Check if this observer is interested in the given event.
     * 
     * EventBus uses this to determine if event should be routed to this observer.
     * Allows for flexible filtering without tight coupling to event type.
     * 
     * Default implementation (can override):
     * - Check event type matches observer's domain
     * - Check observer can handle event severity
     * - Check observer is not disabled/rate-limited
     * 
     * @param event The event to evaluate for handling
     * @return true if observer should handle this event, false otherwise
     */
    boolean canHandle(EventEnvelope event);
}
