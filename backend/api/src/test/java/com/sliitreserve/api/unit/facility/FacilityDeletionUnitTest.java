package com.sliitreserve.api.unit.facility;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.factories.FacilityFactory;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.bookings.CheckInRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.services.facility.FacilityService;
import com.sliitreserve.api.services.facility.FacilityTimetableService;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import com.sliitreserve.api.util.mapping.FacilityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Facility Deletion Unit Tests")
public class FacilityDeletionUnitTest {

    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock
    private UtilizationSnapshotRepository utilizationSnapshotRepository;
    @Mock
    private CheckInRepository checkInRepository;
    @Mock
    private ApprovalStepRepository approvalStepRepository;
    @Mock
    private FacilityFactory facilityFactory;
    @Mock
    private FacilityMapper facilityMapper;
    @Mock
    private MaintenanceIntegrationService maintenanceIntegrationService;
    @Mock
    private FacilityTimetableService facilityTimetableService;
    @Mock
    private EventPublisher notificationService;

    private FacilityService facilityService;

    @BeforeEach
    void setUp() {
        facilityService = new FacilityService(
            facilityRepository, facilityFactory, facilityMapper,
            maintenanceIntegrationService, facilityTimetableService,
            bookingRepository, maintenanceTicketRepository,
            utilizationSnapshotRepository, checkInRepository,
            approvalStepRepository, notificationService
        );
    }

    @Test
    @DisplayName("deleteFacility throws ConflictException when bookings exist and force is false")
    void deleteFacility_ThrowsConflict_WhenBookingsExist() {
        UUID id = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName("Test Gym");

        when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
        when(bookingRepository.countByFacility_Id(id)).thenReturn(5L);

        ConflictException ex = assertThrows(ConflictException.class, () -> 
            facilityService.deleteFacility(id, false)
        );

        assertTrue(ex.getMessage().contains("5 active/scheduled bookings"));
        verify(facilityRepository, never()).delete(any(Facility.class));
    }

    @Test
    @DisplayName("deleteFacility cancels bookings and notifies when force is true")
    void deleteFacility_ForceDelete_CancelsAndNotifies() {
        UUID id = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName("Test Gym");

        User requester = new User();
        requester.setId(UUID.randomUUID());
        
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setStatus(BookingStatus.APPROVED);
        booking.setRequestedBy(requester);
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setStartTime(LocalTime.of(10, 0));

        when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
        when(bookingRepository.countByFacility_Id(id)).thenReturn(1L);
        when(bookingRepository.findByFacility_Id(id)).thenReturn(List.of(booking));

        facilityService.deleteFacility(id, true);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(notificationService).publish(eventCaptor.capture());
        EventEnvelope capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getMetadata());
        assertEquals(requester.getId().toString(), capturedEvent.getMetadata().get("userId"));
        verify(facilityRepository).delete(any(Facility.class));
    }

    @Test
    @DisplayName("markOutOfService cancels active bookings and notifies users")
    void markOutOfService_CancelsBookingsAndNotifies() {
        UUID id = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName("Test Lab");

        User requester = new User();
        requester.setId(UUID.randomUUID());

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setStatus(BookingStatus.PENDING);
        booking.setRequestedBy(requester);
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setStartTime(LocalTime.of(11, 0));

        when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
        when(bookingRepository.findByFacility_Id(id)).thenReturn(List.of(booking));

        facilityService.markOutOfService(id);

        assertEquals(Facility.FacilityStatus.OUT_OF_SERVICE, facility.getStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);

        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(notificationService).publish(eventCaptor.capture());
        EventEnvelope capturedEvent = eventCaptor.getValue();
        assertEquals("FACILITY_REMOVED_CANCELLED", capturedEvent.getEventType());
        assertNotNull(capturedEvent.getMetadata());
        assertEquals(requester.getId().toString(), capturedEvent.getMetadata().get("userId"));
    }

    @Test
    @DisplayName("markOutOfService does not cancel bookings whose start time has already passed")
    void markOutOfService_DoesNotCancelPastStartBookings() {
        UUID id = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName("Test Lab");

        User requester = new User();
        requester.setId(UUID.randomUUID());

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setStatus(BookingStatus.APPROVED);
        booking.setRequestedBy(requester);
        booking.setBookingDate(LocalDate.now().minusDays(1));
        booking.setStartTime(LocalTime.of(9, 0));

        when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
        when(bookingRepository.findByFacility_Id(id)).thenReturn(List.of(booking));

        facilityService.markOutOfService(id);

        assertEquals(BookingStatus.APPROVED, booking.getStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(notificationService, never()).publish(any(EventEnvelope.class));
    }

    @Test
    @DisplayName("deleteFacility performs cascading nuke of associated records")
    void deleteFacility_PerformsCascadeNuke() {
        UUID id = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(id);

        when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
        when(bookingRepository.countByFacility_Id(id)).thenReturn(0L);

        facilityService.deleteFacility(id, false);

        verify(utilizationSnapshotRepository).deleteAll(anyList());
        verify(maintenanceTicketRepository).deleteAll(anyList());
        verify(facilityRepository).delete(any(Facility.class));
    }
}
