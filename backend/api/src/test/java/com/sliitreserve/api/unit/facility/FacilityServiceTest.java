package com.sliitreserve.api.unit.facility;

import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.factories.FacilityFactory;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.services.facility.FacilityService;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.bookings.CheckInRepository;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.observers.EventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sliitreserve.api.util.mapping.FacilityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilityFactory facilityFactory;

    @Mock
    private com.sliitreserve.api.services.facility.FacilityTimetableService facilityTimetableService;

    private FacilityMapper facilityMapper;

    @Mock
    private MaintenanceIntegrationService maintenanceIntegrationService;
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

    private FacilityService facilityService;

    @BeforeEach
    void init() {
        facilityMapper = org.mockito.Mockito.spy(new FacilityMapper());
        facilityService = new FacilityService(
            facilityRepository, facilityFactory, facilityMapper, 
            maintenanceIntegrationService, facilityTimetableService,
            bookingRepository, maintenanceTicketRepository,
            utilizationSnapshotRepository, checkInRepository,
            approvalStepRepository, notificationService
        );
    }

    @Test
    public void searchFacilities_returnsPagedResults() {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setName("Main Hall");
        facility.setType(Facility.FacilityType.LECTURE_HALL);

        FacilityResponseDTO dto = new FacilityResponseDTO();
        dto.setId(facility.getId());
        dto.setName("Main Hall");
        dto.setType(Facility.FacilityType.LECTURE_HALL);

        Page<Facility> page = new PageImpl<>(List.of(facility), PageRequest.of(0, 10), 1);
        when(facilityRepository.findAll((Specification<Facility>) any(), any(Pageable.class))).thenReturn(page);
        when(facilityMapper.toResponseDTO(facility)).thenReturn(dto);

        Page<FacilityResponseDTO> response = facilityService.searchFacilities(
                Facility.FacilityType.LECTURE_HALL,
                100,
                "A Block",
                "North Wing",
                "hall",
                Facility.FacilityStatus.ACTIVE,
                PageRequest.of(0, 10)
        );

        assertEquals(1, response.getTotalElements());
        assertEquals("Main Hall", response.getContent().get(0).getName());
    }

    @Test
    public void createFacility_throwsConflictWhenFacilityCodeExists() {
        FacilityRequestDTO request = new FacilityRequestDTO();
        request.setFacilityCode("F-001");
        request.setName("Main Hall");
        request.setType(Facility.FacilityType.LECTURE_HALL);
        request.setCapacity(120);
        request.setBuilding("A Block");
        request.setLocationDescription("Ground Floor");
        request.setAvailabilityStartTime(LocalTime.of(8, 0));
        request.setAvailabilityEndTime(LocalTime.of(17, 0));

        when(facilityRepository.existsByFacilityCode("F-001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> facilityService.createFacility(request));
        verify(facilityFactory, never()).createFacility(any(FacilityRequestDTO.class));
    }

    @Test
    public void isFacilityOperational_returnsFalseWhenUnderMaintenance() {
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setStatus(Facility.FacilityStatus.ACTIVE);
        facility.setAvailabilityStartTime(LocalTime.of(8, 0));
        facility.setAvailabilityEndTime(LocalTime.of(18, 0));

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(maintenanceIntegrationService.isFacilityUnderMaintenance(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        boolean result = facilityService.isFacilityOperational(
                facilityId,
                LocalDateTime.of(2026, 4, 10, 9, 0),
                LocalDateTime.of(2026, 4, 10, 10, 0)
        );

        assertFalse(result);
    }
}
