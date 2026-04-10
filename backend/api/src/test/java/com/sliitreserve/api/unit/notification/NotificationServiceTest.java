package com.sliitreserve.api.unit.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventPublisher;
import com.sliitreserve.api.observer.EventSeverity;
import com.sliitreserve.api.observer.Observer;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for NotificationService Observer Routing
 *
 * Purpose: Verify that NotificationService correctly routes domain events
 * to registered observers based on severity levels and event types.
 *
 * Test Scope:
 * 1. HIGH severity events route to both InAppObserver and EmailObserver
 * 2. STANDARD severity events route to InAppObserver only
 * 3. Observer filtering via canHandle() method
 * 4. Event dispatch to matching observers
 * 5. Error handling and observer isolation
 * 6. Multiple observer subscription and management
 * 7. Idempotent event handling
 *
 * From Specification Requirements:
 * - FR-051: HIGH severity events (SLA escalations, suspensions) -> email + in-app
 * - FR-052: STANDARD severity events (booking approvals, reminders) -> in-app only
 * - AR-007: Notification fan-out behavior MUST use Observer pattern
 *
 * From Research Decision 8:
 * - Use Observer pattern with InAppObserver and EmailObserver
 * - Severity-based routing: HIGH = email + in-app, STANDARD = in-app only
 * - Clean channel fan-out and extensibility for additional channels
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Observer Routing Tests")
class NotificationServiceTest {

    @Mock
    private Observer inAppObserver;

    @Mock
    private Observer emailObserver;

    @Mock
    private Observer additionalObserver;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private EventEnvelope testEvent;

    @BeforeEach
    void setUp() {
        // Initialize test event with HIGH severity (sends to both observers)
        testEvent = buildEventEnvelope(
                "BOOKING_APPROVED",
                EventSeverity.HIGH,
                123L,
                "Booking Approved",
                "Your booking has been approved"
        );
    }

    /**
     * Helper method to build EventEnvelope for testing
     */
    private EventEnvelope buildEventEnvelope(
            String eventType,
            EventSeverity severity,
            Long affectedUserId,
            String title,
            String description) {
        return EventEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .severity(severity)
                .affectedUserId(affectedUserId)
                .title(title)
                .description(description)
                .source("BookingService")
                .occurrenceTime(ZonedDateTime.now())
                .publishedAt(ZonedDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("High Severity Event Routing")
    class HighSeverityRoutingTests {

        @BeforeEach
        void setUp() {
            // Configure mock observers for HIGH severity routing
            when(inAppObserver.canHandle(any(EventEnvelope.class))).thenReturn(true);
            when(emailObserver.canHandle(any(EventEnvelope.class))).thenReturn(true);
            when(additionalObserver.canHandle(any(EventEnvelope.class))).thenReturn(false);

            // Subscribe observers
            notificationService.subscribe(inAppObserver);
            notificationService.subscribe(emailObserver);
            notificationService.subscribe(additionalObserver);
        }

        @Test
        @DisplayName("Should route HIGH severity event to both InAppObserver and EmailObserver")
        void testHighSeverityRoutsToMultipleObservers() {
            // Arrange
            EventEnvelope highSeverityEvent = buildEventEnvelope(
                    "SLA_DEADLINE_BREACHED",
                    EventSeverity.HIGH,
                    456L,
                    "SLA Deadline Breached",
                    "Critical ticket SLA has been breached"
            );

            // Act
            notificationService.publish(highSeverityEvent);

            // Assert
            verify(inAppObserver, times(1)).handleEvent(highSeverityEvent);
            verify(emailObserver, times(1)).handleEvent(highSeverityEvent);
            verify(additionalObserver, never()).handleEvent(any());
        }

        @Test
        @DisplayName("Should dispatch HIGH severity USER_SUSPENDED event to observers")
        void testHighSeverityUserSuspendedEvent() {
            // Arrange
            EventEnvelope suspensionEvent = buildEventEnvelope(
                    "USER_SUSPENDED",
                    EventSeverity.HIGH,
                    789L,
                    "User Suspended",
                    "You have been suspended due to excessive no-shows"
            );

            // Act
            notificationService.publish(suspensionEvent);

            // Assert
            verify(inAppObserver, times(1)).handleEvent(suspensionEvent);
            verify(emailObserver, times(1)).handleEvent(suspensionEvent);
        }

        @Test
        @DisplayName("Should dispatch HIGH severity TICKET_ESCALATED event to observers")
        void testHighSeverityTicketEscalatedEvent() {
            // Arrange
            EventEnvelope escalationEvent = buildEventEnvelope(
                    "TICKET_ESCALATED",
                    EventSeverity.HIGH,
                    111L,
                    "Ticket Escalated",
                    "Maintenance ticket escalated to facility manager"
            );

            // Act
            notificationService.publish(escalationEvent);

            // Assert
            verify(inAppObserver, times(1)).handleEvent(escalationEvent);
            verify(emailObserver, times(1)).handleEvent(escalationEvent);
        }

        @Test
        @DisplayName("Should pass complete EventEnvelope to all observers")
        void testEventEnvelopeCompleteness() {
            // Arrange
            Set<String> tags = new HashSet<>();
            tags.add("urgent");
            tags.add("escalation");

            EventEnvelope completeEvent = EventEnvelope.builder()
                    .eventId("event-123-uuid")
                    .eventType("CRITICAL_EVENT")
                    .severity(EventSeverity.HIGH)
                    .affectedUserId(999L)
                    .secondaryUserId(888L)
                    .title("Critical Event")
                    .description("This is a critical event requiring both channels")
                    .source("TestService")
                    .entityReference("ticket:42")
                    .actionUrl("/tickets/42")
                    .actionLabel("View Ticket")
                    .tags(tags)
                    .occurrenceTime(ZonedDateTime.now())
                    .publishedAt(ZonedDateTime.now())
                    .build();

            // Act
            notificationService.publish(completeEvent);

            // Assert
            verify(inAppObserver).handleEvent(completeEvent);
            verify(emailObserver).handleEvent(completeEvent);
        }
    }

    @Nested
    @DisplayName("Standard Severity Event Routing")
    class StandardSeverityRoutingTests {

        @BeforeEach
        void setUp() {
            // Configure mock observers for STANDARD severity routing
            when(inAppObserver.canHandle(any(EventEnvelope.class))).thenReturn(true);
            when(emailObserver.canHandle(any(EventEnvelope.class))).thenReturn(false); // Email doesn't handle STANDARD
            when(additionalObserver.canHandle(any(EventEnvelope.class))).thenReturn(false);

            // Subscribe observers
            notificationService.subscribe(inAppObserver);
            notificationService.subscribe(emailObserver);
        }

        @Test
        @DisplayName("Should route STANDARD severity event to InAppObserver only")
        void testStandardSeverityRoutesInAppOnly() {
            // Arrange
            EventEnvelope standardEvent = buildEventEnvelope(
                    "BOOKING_APPROVED",
                    EventSeverity.STANDARD,
                    123L,
                    "Booking Approved",
                    "Your booking has been approved"
            );

            // Act
            notificationService.publish(standardEvent);

            // Assert
            verify(inAppObserver, times(1)).handleEvent(standardEvent);
            verify(emailObserver, never()).handleEvent(any());
        }

        @Test
        @DisplayName("Should dispatch STANDARD severity CHECK_IN_SUCCESS event to InAppObserver only")
        void testStandardSeverityCheckInSuccessEvent() {
            // Arrange
            EventEnvelope checkInEvent = buildEventEnvelope(
                    "CHECK_IN_SUCCESS",
                    EventSeverity.STANDARD,
                    234L,
                    "Check-in Successful",
                    "You have successfully checked in to the facility"
            );

            // Act
            notificationService.publish(checkInEvent);

            // Assert
            verify(inAppObserver, times(1)).handleEvent(checkInEvent);
            verify(emailObserver, never()).handleEvent(any());
        }

        @Test
        @DisplayName("Should dispatch STANDARD severity TICKET_ASSIGNED event to InAppObserver only")
        void testStandardSeverityTicketAssignedEvent() {
            // Arrange
            EventEnvelope assignmentEvent = buildEventEnvelope(
                    "TICKET_ASSIGNED",
                    EventSeverity.STANDARD,
                    345L,
                    "Ticket Assigned",
                    "A maintenance ticket has been assigned to you"
            );

            // Act
            notificationService.publish(assignmentEvent);

            // Assert
            verify(inAppObserver, times(1)).handleEvent(assignmentEvent);
            verify(emailObserver, never()).handleEvent(any());
        }

        @Test
        @DisplayName("Should dispatch STANDARD severity BOOKING_REJECTED event")
        void testStandardSeverityBookingRejectedEvent() {
            // Arrange
            EventEnvelope rejectionEvent = buildEventEnvelope(
                    "BOOKING_REJECTED",
                    EventSeverity.STANDARD,
                    456L,
                    "Booking Rejected",
                    "Your booking request has been rejected"
            );

            // Act
            notificationService.publish(rejectionEvent);

            // Assert
            verify(inAppObserver, times(1)).handleEvent(rejectionEvent);
            verify(emailObserver, never()).handleEvent(any());
        }
    }

    @Nested
    @DisplayName("Observer Filtering via canHandle()")
    class ObserverFilteringTests {

        @BeforeEach
        void setUp() {
            notificationService.subscribe(inAppObserver);
            notificationService.subscribe(emailObserver);
        }

        @Test
        @DisplayName("Should only call handleEvent() on observers that return true from canHandle()")
        void testObserverFilteringByCanHandle() {
            // Arrange
            when(inAppObserver.canHandle(any())).thenReturn(true);
            when(emailObserver.canHandle(any())).thenReturn(false);

            EventEnvelope event = buildEventEnvelope(
                    "TEST_EVENT",
                    EventSeverity.STANDARD,
                    111L,
                    "Test Event",
                    "Test"
            );

            // Act
            notificationService.publish(event);

            // Assert
            verify(inAppObserver).canHandle(event);
            verify(emailObserver).canHandle(event);
            verify(inAppObserver).handleEvent(event);
            verify(emailObserver, never()).handleEvent(any());
        }

        @Test
        @DisplayName("Should skip handlers that return false from canHandle()")
        void testSkipHandlersThatCannotHandle() {
            // Arrange
            when(inAppObserver.canHandle(any())).thenReturn(false);
            when(emailObserver.canHandle(any())).thenReturn(false);

            EventEnvelope event = buildEventEnvelope(
                    "UNHANDLED_EVENT",
                    EventSeverity.HIGH,
                    222L,
                    "Unhandled Event",
                    "No observers can handle this"
            );

            // Act
            notificationService.publish(event);

            // Assert
            verify(inAppObserver, never()).handleEvent(any());
            verify(emailObserver, never()).handleEvent(any());
        }

        @Test
        @DisplayName("Should evaluate canHandle() for each event (not cached)")
        void testCanHandleEvaluatedPerEvent() {
            // Arrange
            when(inAppObserver.canHandle(any())).thenReturn(true);

            EventEnvelope event1 = buildEventEnvelope(
                    "EVENT_1",
                    EventSeverity.STANDARD,
                    111L,
                    "Event 1",
                    "First event"
            );

            EventEnvelope event2 = buildEventEnvelope(
                    "EVENT_2",
                    EventSeverity.STANDARD,
                    222L,
                    "Event 2",
                    "Second event"
            );

            // Act
            notificationService.publish(event1);
            notificationService.publish(event2);

            // Assert - canHandle should be called for each event
            verify(inAppObserver, times(2)).canHandle(any());
            verify(inAppObserver).handleEvent(event1);
            verify(inAppObserver).handleEvent(event2);
        }
    }

    @Nested
    @DisplayName("Observer Subscription Management")
    class SubscriptionManagementTests {

        @Test
        @DisplayName("Should register observer via subscribe()")
        void testObserverSubscription() {
            // Arrange
            when(inAppObserver.canHandle(any())).thenReturn(true);

            // Act
            notificationService.subscribe(inAppObserver);
            notificationService.publish(testEvent);

            // Assert
            verify(inAppObserver).handleEvent(testEvent);
        }

        @Test
        @DisplayName("Should unregister observer via unsubscribe()")
        void testObserverUnsubscription() {
            // Arrange
            when(inAppObserver.canHandle(any())).thenReturn(true);
            notificationService.subscribe(inAppObserver);

            // Act
            notificationService.unsubscribe(inAppObserver);
            notificationService.publish(testEvent);

            // Assert
            verify(inAppObserver, never()).handleEvent(any());
        }

        @Test
        @DisplayName("Should support multiple observer registrations")
        void testMultipleObserverSubscriptions() {
            // Arrange
            when(inAppObserver.canHandle(any())).thenReturn(true);
            when(emailObserver.canHandle(any())).thenReturn(true);
            when(additionalObserver.canHandle(any())).thenReturn(true);

            // Act
            notificationService.subscribe(inAppObserver);
            notificationService.subscribe(emailObserver);
            notificationService.subscribe(additionalObserver);
            notificationService.publish(testEvent);

            // Assert
            verify(inAppObserver).handleEvent(testEvent);
            verify(emailObserver).handleEvent(testEvent);
            verify(additionalObserver).handleEvent(testEvent);
        }

        @Test
        @DisplayName("Should allow re-subscription of previously unsubscribed observer")
        void testResubscriptionAfterUnsubscribe() {
            // Arrange
            when(inAppObserver.canHandle(any())).thenReturn(true);

            // Act
            notificationService.subscribe(inAppObserver);
            notificationService.unsubscribe(inAppObserver);
            notificationService.subscribe(inAppObserver); // Re-subscribe
            notificationService.publish(testEvent);

            // Assert
            verify(inAppObserver).handleEvent(testEvent);
        }
    }

    @Nested
    @DisplayName("Error Handling and Observer Isolation")
    class ErrorHandlingTests {

        @BeforeEach
        void setUp() {
            when(inAppObserver.canHandle(any())).thenReturn(true);
            when(emailObserver.canHandle(any())).thenReturn(true);

            notificationService.subscribe(inAppObserver);
            notificationService.subscribe(emailObserver);
        }

        @Test
        @DisplayName("Should handle exceptions in one observer without affecting others")
        void testObserverIsolationOnException() {
            // Arrange
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailObserver).handleEvent(any());

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> notificationService.publish(testEvent));

            // Both observers should be attempted
            verify(inAppObserver).handleEvent(testEvent);
            verify(emailObserver).handleEvent(testEvent);
        }

        @Test
        @DisplayName("Should continue processing after observer failure")
        void testContinueAfterObserverFailure() {
            // Arrange
            doThrow(new RuntimeException("Processing failed"))
                    .when(inAppObserver).handleEvent(any());

            // Act
            assertDoesNotThrow(() -> notificationService.publish(testEvent));

            // Assert - email observer should still be called
            verify(emailObserver).handleEvent(testEvent);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null event")
        void testNullEventHandling() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                notificationService.publish(null);
            });
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for event with no event type")
        void testEventWithoutTypeHandling() {
            // Arrange
            EventEnvelope invalidEvent = EventEnvelope.builder()
                    .eventId("event-123")
                    .severity(EventSeverity.STANDARD)
                    .affectedUserId(123L)
                    .title("Incomplete Event")
                    // eventType is missing
                    .build();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                notificationService.publish(invalidEvent);
            });
        }
    }

    @Nested
    @DisplayName("Event Type Routing")
    class EventTypeRoutingTests {

        @BeforeEach
        void setUp() {
            when(inAppObserver.canHandle(any())).thenReturn(true);
            when(emailObserver.canHandle(any())).thenReturn(true);

            notificationService.subscribe(inAppObserver);
            notificationService.subscribe(emailObserver);
        }

        @Test
        @DisplayName("Should route BOOKING_REQUEST_SUBMITTED event")
        void testBookingRequestSubmittedRouting() {
            // Arrange
            EventEnvelope event = buildEventEnvelope(
                    "BOOKING_REQUEST_SUBMITTED",
                    EventSeverity.STANDARD,
                    100L,
                    "Booking Request Submitted",
                    "Your booking request has been submitted for approval"
            );

            // Act
            notificationService.publish(event);

            // Assert
            verify(inAppObserver).handleEvent(event);
        }

        @Test
        @DisplayName("Should route SLA_DEADLINE_APPROACHING event as HIGH severity")
        void testSlaDeadlineApproachingRouting() {
            // Arrange
            EventEnvelope event = buildEventEnvelope(
                    "SLA_DEADLINE_APPROACHING",
                    EventSeverity.HIGH,
                    200L,
                    "SLA Deadline Approaching",
                    "SLA deadline is approaching for assigned ticket"
            );

            // Act
            notificationService.publish(event);

            // Assert
            verify(inAppObserver).handleEvent(event);
            verify(emailObserver).handleEvent(event);
        }

        @Test
        @DisplayName("Should route NO_SHOW_RECORDED event")
        void testNoShowRecordedRouting() {
            // Arrange
            EventEnvelope event = buildEventEnvelope(
                    "NO_SHOW_RECORDED",
                    EventSeverity.STANDARD,
                    300L,
                    "No-Show Recorded",
                    "Your no-show has been recorded. No-show count: 1"
            );

            // Act
            notificationService.publish(event);

            // Assert
            verify(inAppObserver).handleEvent(event);
        }

        @Test
        @DisplayName("Should route APPEAL_SUBMITTED event")
        void testAppealSubmittedRouting() {
            // Arrange
            EventEnvelope event = buildEventEnvelope(
                    "APPEAL_SUBMITTED",
                    EventSeverity.STANDARD,
                    400L,
                    "Appeal Submitted",
                    "Your suspension appeal has been submitted"
            );

            // Act
            notificationService.publish(event);

            // Assert
            verify(inAppObserver).handleEvent(event);
        }
    }

    @Nested
    @DisplayName("Idempotency and Deduplication")
    class IdempotencyTests {

        @BeforeEach
        void setUp() {
            when(inAppObserver.canHandle(any())).thenReturn(true);
            notificationService.subscribe(inAppObserver);
        }

        @Test
        @DisplayName("Should handle duplicate event publications safely")
        void testDuplicateEventHandling() {
            // Arrange - same event published twice
            EventEnvelope event = buildEventEnvelope(
                    "TEST_EVENT",
                    EventSeverity.STANDARD,
                    500L,
                    "Test Event",
                    "Test"
            );

            // Act
            notificationService.publish(event);
            notificationService.publish(event); // Publish same event again

            // Assert - both should be handled (no deduplication in service level)
            verify(inAppObserver, times(2)).handleEvent(event);
        }

        @Test
        @DisplayName("Should allow events with same type but different IDs")
        void testSimilarEventDifferentIds() {
            // Arrange
            EventEnvelope event1 = buildEventEnvelope(
                    "BOOKING_APPROVED",
                    EventSeverity.STANDARD,
                    600L,
                    "Booking Approved",
                    "Booking 1 approved"
            );

            EventEnvelope event2 = buildEventEnvelope(
                    "BOOKING_APPROVED",
                    EventSeverity.STANDARD,
                    600L,
                    "Booking Approved",
                    "Booking 2 approved"
            );

            // Act
            notificationService.publish(event1);
            notificationService.publish(event2);

            // Assert
            verify(inAppObserver, times(2)).handleEvent(any());
        }
    }
}
