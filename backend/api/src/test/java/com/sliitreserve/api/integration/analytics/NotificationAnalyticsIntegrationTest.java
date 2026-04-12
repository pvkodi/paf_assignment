package com.sliitreserve.api.integration.analytics;

import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.observers.Observer;
import com.sliitreserve.api.repositories.BookingRepository;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.repositories.UtilizationSnapshotRepository;
import com.sliitreserve.api.services.analytics.UtilizationSnapshotService;
import com.sliitreserve.api.services.notification.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification and Analytics Integration Tests")
class NotificationAnalyticsIntegrationTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UtilizationSnapshotRepository snapshotRepository;

    private NotificationServiceImpl notificationService;
    private UtilizationSnapshotService utilizationSnapshotService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl();
        utilizationSnapshotService = new UtilizationSnapshotService(
            facilityRepository,
            bookingRepository,
            snapshotRepository,
            ZoneId.of("Asia/Colombo")
        );
    }

    @Test
    @DisplayName("HIGH severity events route to all matching observers")
    void highSeverityEvent_routesToAllObservers() {
        Observer inAppObserver = mock(Observer.class);
        Observer emailObserver = mock(Observer.class);

        when(inAppObserver.canHandle(any(EventEnvelope.class))).thenReturn(true);
        when(emailObserver.canHandle(any(EventEnvelope.class))).thenReturn(true);

        notificationService.subscribe(inAppObserver);
        notificationService.subscribe(emailObserver);

        EventEnvelope event = EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("SLA_DEADLINE_BREACHED")
            .severity(EventSeverity.HIGH)
            .title("SLA breached")
            .description("A critical SLA threshold has been exceeded")
            .source("TicketService")
            .occurrenceTime(ZonedDateTime.now())
            .build();

        notificationService.publish(event);

        verify(inAppObserver).handleEvent(event);
        verify(emailObserver).handleEvent(event);
        assertTrue(event.getTags().contains("high"));

        notificationService.unsubscribe(inAppObserver);
        notificationService.unsubscribe(emailObserver);
    }

    @Test
    @DisplayName("STANDARD severity events skip observers that opt out")
    void standardSeverityEvent_routesOnlyToInterestedObservers() {
        Observer inAppObserver = mock(Observer.class);
        Observer emailObserver = mock(Observer.class);

        when(inAppObserver.canHandle(any(EventEnvelope.class))).thenReturn(true);
        when(emailObserver.canHandle(any(EventEnvelope.class))).thenReturn(false);

        notificationService.subscribe(inAppObserver);
        notificationService.subscribe(emailObserver);

        EventEnvelope event = EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("BOOKING_APPROVED")
            .severity(EventSeverity.STANDARD)
            .title("Booking approved")
            .description("Your booking was approved")
            .source("BookingService")
            .occurrenceTime(ZonedDateTime.now())
            .build();

        notificationService.publish(event);

        verify(inAppObserver).handleEvent(event);
        verify(emailObserver, never()).handleEvent(any(EventEnvelope.class));
        assertTrue(event.getTags().contains("standard"));

        notificationService.unsubscribe(inAppObserver);
        notificationService.unsubscribe(emailObserver);
    }

    @Test
    @DisplayName("daily snapshot generation persists calculated utilization")
    void generateDailySnapshots_persistsCalculatedSnapshot() {
        LocalDate snapshotDate = LocalDate.of(2026, 4, 30);

        UUID facilityId = UUID.randomUUID();
        Facility activeFacility = new Facility();
        activeFacility.setId(facilityId);
        activeFacility.setName("Main Hall");
        activeFacility.setStatus(FacilityStatus.ACTIVE);
        activeFacility.setAvailabilityStart(LocalTime.of(8, 0));
        activeFacility.setAvailabilityEnd(LocalTime.of(18, 0));

        User requester = User.builder()
            .id(UUID.randomUUID())
            .googleSubject("google-sub")
            .email("student@smartcampus.local")
            .displayName("Student")
            .build();

        Booking approvedBooking = Booking.builder()
            .id(UUID.randomUUID())
            .facility(activeFacility)
            .requestedBy(requester)
            .bookedFor(requester)
            .bookingDate(snapshotDate)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(11, 30))
            .purpose("Workshop")
            .attendees(30)
            .status(BookingStatus.APPROVED)
            .build();

        when(facilityRepository.findByStatus(FacilityStatus.ACTIVE)).thenReturn(List.of(activeFacility));
        when(bookingRepository.findByFacility_IdAndBookingDateAndStatusIn(eq(facilityId), eq(snapshotDate), anyList()))
            .thenReturn(List.of(approvedBooking));
        when(snapshotRepository.findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());
        when(snapshotRepository.findByFacility_IdAndSnapshotDate(facilityId, snapshotDate)).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(UtilizationSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processedCount = utilizationSnapshotService.generateDailySnapshots(snapshotDate);

        assertEquals(1, processedCount);

        ArgumentCaptor<UtilizationSnapshot> snapshotCaptor = ArgumentCaptor.forClass(UtilizationSnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());

        UtilizationSnapshot saved = snapshotCaptor.getValue();
        assertEquals(new BigDecimal("10.00"), saved.getAvailableHours());
        assertEquals(new BigDecimal("2.50"), saved.getBookedHours());
        assertEquals(new BigDecimal("25.00"), saved.getUtilizationPercent());
        assertFalse(saved.isUnderutilized());
        assertEquals(0, saved.getConsecutiveUnderutilizedDays());
    }

    @Test
    @DisplayName("daily snapshot generation ignores facilities with invalid availability window")
    void generateDailySnapshots_skipsInvalidFacilityWindow() {
        LocalDate snapshotDate = LocalDate.of(2026, 4, 30);

        Facility invalidFacility = new Facility();
        invalidFacility.setId(UUID.randomUUID());
        invalidFacility.setName("Invalid Room");
        invalidFacility.setStatus(FacilityStatus.ACTIVE);
        invalidFacility.setAvailabilityStart(LocalTime.of(10, 0));
        invalidFacility.setAvailabilityEnd(LocalTime.of(10, 0));

        when(facilityRepository.findByStatus(FacilityStatus.ACTIVE)).thenReturn(List.of(invalidFacility));

        int processedCount = utilizationSnapshotService.generateDailySnapshots(snapshotDate);

        assertEquals(0, processedCount);
        verify(snapshotRepository, never()).save(any(UtilizationSnapshot.class));
    }
}
