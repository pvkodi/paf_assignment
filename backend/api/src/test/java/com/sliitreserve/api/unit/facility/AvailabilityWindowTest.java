package com.sliitreserve.api.unit.facility;

import com.sliitreserve.api.dto.facility.AvailabilityWindowDTO;
import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.AvailabilityWindow;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.factories.FacilityFactory;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.services.facility.FacilityService;
import com.sliitreserve.api.services.facility.FacilityTimetableService;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import com.sliitreserve.api.util.mapping.FacilityMapper;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.bookings.CheckInRepository;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.observers.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AvailabilityWindow schedule model and its integration
 * with FacilityService.isFacilityOperational().
 *
 * Covers:
 *  - AvailabilityWindow.contains() boundary conditions
 *  - Facility.isAvailableAt() multi-window logic
 *  - FacilityService.isFacilityOperational() using the new schedule
 *  - FacilityService.createFacility() persists availability windows
 *  - FacilityMapper round-trip for availability windows
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Availability Window Schedule Tests")
public class AvailabilityWindowTest {

    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private FacilityFactory facilityFactory;
    @Mock
    private MaintenanceIntegrationService maintenanceIntegrationService;
    @Mock
    private FacilityTimetableService facilityTimetableService;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CheckInRepository checkInRepository;
    @Mock
    private ApprovalStepRepository approvalStepRepository;
    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock
    private UtilizationSnapshotRepository utilizationSnapshotRepository;
    @Mock
    private EventPublisher notificationService;

    private FacilityMapper facilityMapper;
    private FacilityService facilityService;

    @BeforeEach
    void setUp() {
        facilityMapper = spy(new FacilityMapper());
        facilityService = new FacilityService(
                facilityRepository, facilityFactory, facilityMapper,
                maintenanceIntegrationService, facilityTimetableService,
                bookingRepository, maintenanceTicketRepository, 
                utilizationSnapshotRepository, checkInRepository,
                approvalStepRepository, notificationService
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AvailabilityWindow.contains() unit tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AvailabilityWindow.contains()")
    class AvailabilityWindowContainsTests {

        private final AvailabilityWindow mondayMorning = AvailabilityWindow.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        @Test
        @DisplayName("returns true when day and time are within the window")
        void returnsTrue_whenDayAndTimeMatch() {
            assertTrue(mondayMorning.contains(DayOfWeek.MONDAY, LocalTime.of(10, 0)));
        }

        @Test
        @DisplayName("returns true at exact start time (inclusive)")
        void returnsTrue_atExactStart() {
            assertTrue(mondayMorning.contains(DayOfWeek.MONDAY, LocalTime.of(8, 0)));
        }

        @Test
        @DisplayName("returns false at exact end time (exclusive)")
        void returnsFalse_atExactEnd() {
            assertFalse(mondayMorning.contains(DayOfWeek.MONDAY, LocalTime.of(12, 0)));
        }

        @Test
        @DisplayName("returns false before start time")
        void returnsFalse_beforeStart() {
            assertFalse(mondayMorning.contains(DayOfWeek.MONDAY, LocalTime.of(7, 59)));
        }

        @Test
        @DisplayName("returns false after end time")
        void returnsFalse_afterEnd() {
            assertFalse(mondayMorning.contains(DayOfWeek.MONDAY, LocalTime.of(12, 1)));
        }

        @Test
        @DisplayName("returns false when day does not match")
        void returnsFalse_wrongDay() {
            assertFalse(mondayMorning.contains(DayOfWeek.TUESDAY, LocalTime.of(10, 0)));
        }

        @Test
        @DisplayName("returns false when time is null")
        void returnsFalse_nullTime() {
            assertFalse(mondayMorning.contains(DayOfWeek.MONDAY, null));
        }

        @Test
        @DisplayName("returns false when day is null")
        void returnsFalse_nullDay() {
            assertFalse(mondayMorning.contains(null, LocalTime.of(10, 0)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Facility.isAvailableAt() tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Facility.isAvailableAt()")
    class FacilityIsAvailableAtTests {

        @Test
        @DisplayName("returns true when time falls within any window")
        void returnsTrue_whenTimeMatchesAnyWindow() {
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.MONDAY, 8, 12),
                    window(DayOfWeek.MONDAY, 14, 18)
            );
            assertTrue(facility.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(15, 0)));
        }

        @Test
        @DisplayName("returns false during gap between two windows on same day")
        void returnsFalse_duringGapBetweenWindows() {
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.MONDAY, 8, 12),
                    window(DayOfWeek.MONDAY, 14, 18)
            );
            assertFalse(facility.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(13, 0)));
        }

        @Test
        @DisplayName("returns false when no window exists for the given day")
        void returnsFalse_noWindowForDay() {
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.MONDAY, 8, 18)
            );
            assertFalse(facility.isAvailableAt(DayOfWeek.SUNDAY, LocalTime.of(10, 0)));
        }

        @Test
        @DisplayName("supports Saturday availability window")
        void returnsTrue_forSaturdayWindow() {
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.SATURDAY, 10, 14)
            );
            assertTrue(facility.isAvailableAt(DayOfWeek.SATURDAY, LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("supports Sunday availability window")
        void returnsTrue_forSundayWindow() {
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.SUNDAY, 8, 16)
            );
            assertTrue(facility.isAvailableAt(DayOfWeek.SUNDAY, LocalTime.of(9, 0)));
        }

        @Test
        @DisplayName("falls back to legacy flat times when no windows configured")
        void returnsTrue_legacyFallbackWhenNoWindows() {
            Facility facility = new Facility();
            facility.setAvailabilityStartTime(LocalTime.of(8, 0));
            facility.setAvailabilityEndTime(LocalTime.of(18, 0));
            // availabilityWindows is empty by default

            assertTrue(facility.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(10, 0)));
        }

        @Test
        @DisplayName("returns true for all days when using legacy flat times (no day restriction)")
        void returnsTrue_legacyFallbackAllDays() {
            Facility facility = new Facility();
            facility.setAvailabilityStartTime(LocalTime.of(8, 0));
            facility.setAvailabilityEndTime(LocalTime.of(18, 0));

            // Legacy flat fields carry no day restriction
            assertTrue(facility.isAvailableAt(DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FacilityService.isFacilityOperational() with availability windows
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FacilityService.isFacilityOperational() with schedule windows")
    class IsFacilityOperationalTests {

        @Test
        @DisplayName("returns false when facility is OUT_OF_SERVICE regardless of windows")
        void returnsFalse_outOfService() {
            UUID id = UUID.randomUUID();
            Facility facility = buildFacilityWithWindows(window(DayOfWeek.MONDAY, 8, 18));
            facility.setStatus(Facility.FacilityStatus.OUT_OF_SERVICE);
            facility.setId(id);
            when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));

            boolean result = facilityService.isFacilityOperational(
                    id,
                    LocalDateTime.of(2026, 4, 20, 9, 0),   // Monday
                    LocalDateTime.of(2026, 4, 20, 10, 0)
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("returns false when facility is in MAINTENANCE")
        void returnsFalse_maintenance() {
            UUID id = UUID.randomUUID();
            Facility facility = buildFacilityWithWindows(window(DayOfWeek.MONDAY, 8, 18));
            facility.setStatus(Facility.FacilityStatus.MAINTENANCE);
            facility.setId(id);
            when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));

            boolean result = facilityService.isFacilityOperational(
                    id,
                    LocalDateTime.of(2026, 4, 20, 9, 0),
                    LocalDateTime.of(2026, 4, 20, 10, 0)
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("returns false when requested hour falls outside all windows")
        void returnsFalse_outsideAllWindows() {
            UUID id = UUID.randomUUID();
            // Only available Mon 08:00–12:00 and Mon 14:00–18:00
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.MONDAY, 8, 12),
                    window(DayOfWeek.MONDAY, 14, 18)
            );
            facility.setStatus(Facility.FacilityStatus.ACTIVE);
            facility.setFacilityCode("F-001");
            facility.setId(id);
            when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
            // Note: timetableService stub omitted — isAvailableAt returns false first (short-circuit)

            // 13:00 is in the lunch gap
            boolean result = facilityService.isFacilityOperational(
                    id,
                    LocalDateTime.of(2026, 4, 20, 13, 0),
                    LocalDateTime.of(2026, 4, 20, 14, 0)
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("returns true when requested range falls fully within a window")
        void returnsTrue_withinWindow() {
            UUID id = UUID.randomUUID();
            Facility facility = buildFacilityWithWindows(window(DayOfWeek.MONDAY, 8, 18));
            facility.setStatus(Facility.FacilityStatus.ACTIVE);
            facility.setFacilityCode("F-001");
            facility.setId(id);
            when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
            when(maintenanceIntegrationService.isFacilityUnderMaintenance(any(), any(), any())).thenReturn(false);
            when(facilityTimetableService.isOccupied(any(), any(), any())).thenReturn(false);

            boolean result = facilityService.isFacilityOperational(
                    id,
                    LocalDateTime.of(2026, 4, 20, 9, 0),
                    LocalDateTime.of(2026, 4, 20, 11, 0)
            );

            assertTrue(result);
        }

        @Test
        @DisplayName("returns false when requested day has no window (e.g. Sunday for a weekday facility)")
        void returnsFalse_noWindowOnRequestedDay() {
            UUID id = UUID.randomUUID();
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.MONDAY, 8, 18),
                    window(DayOfWeek.TUESDAY, 8, 18),
                    window(DayOfWeek.WEDNESDAY, 8, 18),
                    window(DayOfWeek.THURSDAY, 8, 18),
                    window(DayOfWeek.FRIDAY, 8, 18)
            );
            facility.setStatus(Facility.FacilityStatus.ACTIVE);
            facility.setFacilityCode("F-001");
            facility.setId(id);
            when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
            // timetableService stub omitted — isAvailableAt returns false for Sunday (short-circuit)

            // Sunday – no window configured
            boolean result = facilityService.isFacilityOperational(
                    id,
                    LocalDateTime.of(2026, 4, 19, 10, 0),  // Sunday
                    LocalDateTime.of(2026, 4, 19, 11, 0)
            );

            assertFalse(result);
        }

        @Test
        @DisplayName("returns false when facility is under maintenance ticket")
        void returnsFalse_underMaintenanceTicker() {
            UUID id = UUID.randomUUID();
            Facility facility = buildFacilityWithWindows(window(DayOfWeek.MONDAY, 8, 18));
            facility.setStatus(Facility.FacilityStatus.ACTIVE);
            facility.setFacilityCode("F-001");
            facility.setId(id);
            when(facilityRepository.findById(id)).thenReturn(Optional.of(facility));
            when(facilityTimetableService.isOccupied(any(), any(), any())).thenReturn(false);
            when(maintenanceIntegrationService.isFacilityUnderMaintenance(any(), any(), any())).thenReturn(true);

            boolean result = facilityService.isFacilityOperational(
                    id,
                    LocalDateTime.of(2026, 4, 20, 9, 0),
                    LocalDateTime.of(2026, 4, 20, 10, 0)
            );

            assertFalse(result);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FacilityMapper round-trip tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FacilityMapper availability windows round-trip")
    class FacilityMapperWindowTests {

        @Test
        @DisplayName("toResponseDTO maps all windows correctly")
        void toResponseDTO_mapsWindowsCorrectly() {
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.MONDAY, 8, 12),
                    window(DayOfWeek.MONDAY, 14, 18),
                    window(DayOfWeek.SATURDAY, 10, 14)
            );
            facility.setId(UUID.randomUUID());
            facility.setName("Test Lab");
            facility.setType(Facility.FacilityType.LAB);
            facility.setCapacity(40);
            facility.setStatus(Facility.FacilityStatus.ACTIVE);
            facility.setAvailabilityStartTime(LocalTime.of(8, 0));
            facility.setAvailabilityEndTime(LocalTime.of(18, 0));

            FacilityMapper mapper = new FacilityMapper();
            FacilityResponseDTO dto = mapper.toResponseDTO(facility);

            assertNotNull(dto.getAvailabilityWindows());
            assertEquals(3, dto.getAvailabilityWindows().size());

            AvailabilityWindowDTO monday1 = dto.getAvailabilityWindows().get(0);
            assertEquals(DayOfWeek.MONDAY, monday1.getDayOfWeek());
            assertEquals(LocalTime.of(8, 0), monday1.getStartTime());
            assertEquals(LocalTime.of(12, 0), monday1.getEndTime());
        }

        @Test
        @DisplayName("updateEntity replaces existing windows with new ones")
        void updateEntity_replacesWindowsCorrectly() {
            Facility facility = buildFacilityWithWindows(
                    window(DayOfWeek.MONDAY, 8, 18)
            );

            FacilityRequestDTO request = new FacilityRequestDTO();
            request.setName("Updated Lab");
            request.setType(Facility.FacilityType.LAB);
            request.setCapacity(50);
            request.setBuilding("Block B");
            request.setLocationDescription("Second floor");
            request.setAvailabilityStartTime(LocalTime.of(8, 0));
            request.setAvailabilityEndTime(LocalTime.of(18, 0));
            request.setAvailabilityWindows(List.of(
                    new AvailabilityWindowDTO(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    new AvailabilityWindowDTO(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(14, 0))
            ));

            FacilityMapper mapper = new FacilityMapper();
            mapper.updateEntity(request, facility);

            assertEquals(2, facility.getAvailabilityWindows().size());
            assertEquals(DayOfWeek.WEDNESDAY, facility.getAvailabilityWindows().get(0).getDayOfWeek());
            assertEquals(DayOfWeek.SATURDAY, facility.getAvailabilityWindows().get(1).getDayOfWeek());
        }

        @Test
        @DisplayName("updateEntity clears windows when empty list is provided")
        void updateEntity_clearsWindowsWhenEmptyListProvided() {
            Facility facility = buildFacilityWithWindows(window(DayOfWeek.MONDAY, 8, 18));

            FacilityRequestDTO request = new FacilityRequestDTO();
            request.setName("Lab");
            request.setType(Facility.FacilityType.LAB);
            request.setCapacity(30);
            request.setBuilding("Block A");
            request.setLocationDescription("Floor 1");
            request.setAvailabilityStartTime(LocalTime.of(8, 0));
            request.setAvailabilityEndTime(LocalTime.of(18, 0));
            request.setAvailabilityWindows(new ArrayList<>());

            FacilityMapper mapper = new FacilityMapper();
            mapper.updateEntity(request, facility);

            assertTrue(facility.getAvailabilityWindows().isEmpty());
        }

        @Test
        @DisplayName("toResponseDTO returns empty windows list when entity has no windows")
        void toResponseDTO_returnsEmptyListWhenNoWindows() {
            Facility facility = new Facility();
            facility.setId(UUID.randomUUID());
            facility.setName("Room A");
            facility.setType(Facility.FacilityType.MEETING_ROOM);
            facility.setCapacity(10);
            facility.setStatus(Facility.FacilityStatus.ACTIVE);
            facility.setAvailabilityStartTime(LocalTime.of(8, 0));
            facility.setAvailabilityEndTime(LocalTime.of(18, 0));

            FacilityMapper mapper = new FacilityMapper();
            FacilityResponseDTO dto = mapper.toResponseDTO(facility);

            assertNotNull(dto.getAvailabilityWindows());
            assertTrue(dto.getAvailabilityWindows().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FacilityService.createFacility() window persistence tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FacilityService.createFacility() — window persistence")
    class CreateFacilityWindowTests {

        @Test
        @DisplayName("createFacility persists provided availability windows")
        void createFacility_persistsAvailabilityWindows() {
            FacilityRequestDTO request = new FacilityRequestDTO();
            request.setFacilityCode("LAB-001");  // explicit code triggers existsByFacilityCode check
            request.setName("New Lab");
            request.setType(Facility.FacilityType.LAB);
            request.setCapacity(40);
            request.setBuilding("Block C");
            request.setLocationDescription("Level 2");
            request.setAvailabilityStartTime(LocalTime.of(8, 0));
            request.setAvailabilityEndTime(LocalTime.of(18, 0));
            request.setAvailabilityWindows(List.of(
                    new AvailabilityWindowDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                    new AvailabilityWindowDTO(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)),
                    new AvailabilityWindowDTO(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(14, 0))
            ));

            Facility builtFacility = new Facility();
            builtFacility.setId(UUID.randomUUID());
            builtFacility.setName("New Lab");
            builtFacility.setType(Facility.FacilityType.LAB);
            builtFacility.setCapacity(40);
            builtFacility.setStatus(Facility.FacilityStatus.ACTIVE);
            builtFacility.setAvailabilityStartTime(LocalTime.of(8, 0));
            builtFacility.setAvailabilityEndTime(LocalTime.of(18, 0));

            when(facilityRepository.existsByFacilityCode("LAB-001")).thenReturn(false);
            when(facilityFactory.createFacility(any())).thenReturn(builtFacility);
            when(facilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FacilityResponseDTO response = facilityService.createFacility(request);

            // Verify save was called and windows were added before saving
            verify(facilityRepository).save(argThat(f ->
                    f.getAvailabilityWindows() != null &&
                    f.getAvailabilityWindows().size() == 3
            ));
        }

        @Test
        @DisplayName("createFacility proceeds without windows when none are provided")
        void createFacility_succeedsWithNoWindows() {
            FacilityRequestDTO request = new FacilityRequestDTO();
            request.setName("Room");
            request.setType(Facility.FacilityType.MEETING_ROOM);
            request.setCapacity(10);
            request.setBuilding("Block A");
            request.setLocationDescription("Floor 1");
            request.setAvailabilityStartTime(LocalTime.of(8, 0));
            request.setAvailabilityEndTime(LocalTime.of(18, 0));
            // no availabilityWindows set — facilityCode is null so existsByFacilityCode path is skipped

            Facility builtFacility = new Facility();
            builtFacility.setId(UUID.randomUUID());
            builtFacility.setName("Room");
            builtFacility.setType(Facility.FacilityType.MEETING_ROOM);
            builtFacility.setCapacity(10);
            builtFacility.setStatus(Facility.FacilityStatus.ACTIVE);
            builtFacility.setAvailabilityStartTime(LocalTime.of(8, 0));
            builtFacility.setAvailabilityEndTime(LocalTime.of(18, 0));

            when(facilityFactory.createFacility(any())).thenReturn(builtFacility);
            when(facilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> facilityService.createFacility(request));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static AvailabilityWindow window(DayOfWeek day, int startHour, int endHour) {
        return AvailabilityWindow.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(endHour, 0))
                .build();
    }

    private static Facility buildFacilityWithWindows(AvailabilityWindow... windows) {
        Facility facility = new Facility();
        facility.setAvailabilityWindows(new ArrayList<>(List.of(windows)));
        return facility;
    }
}
