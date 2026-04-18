package com.sliitreserve.api.unit.facility;

import com.sliitreserve.api.entities.facility.*;
import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.factories.FacilityFactory;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.services.facility.FacilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.bookings.CheckInRepository;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.services.facility.FacilityTimetableService;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import com.sliitreserve.api.util.mapping.FacilityMapper;
import com.sliitreserve.api.observers.EventPublisher;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FacilityServiceExpandedTest {

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

    @InjectMocks
    private FacilityService facilityService;

    @Test
    public void factory_creates_correct_subclasses() {
        FacilityRequestDTO r1 = new FacilityRequestDTO();
        r1.setType(Facility.FacilityType.LECTURE_HALL);
        r1.setFacilityCode("L1");
        r1.setName("Hall");
        r1.setCapacity(100);
        r1.setBuilding("B1");
        Facility f1 = new FacilityFactory().createFacility(r1);
        assertTrue(f1 instanceof LectureHall);

        FacilityRequestDTO r2 = new FacilityRequestDTO();
        r2.setType(Facility.FacilityType.LAB);
        r2.setFacilityCode("LB1");
        r2.setName("Chem Lab");
        r2.setCapacity(30);
        r2.setBuilding("B2");
        Facility f2 = new FacilityFactory().createFacility(r2);
        assertTrue(f2 instanceof Lab);

        FacilityRequestDTO r3 = new FacilityRequestDTO();
        r3.setType(Facility.FacilityType.EQUIPMENT);
        r3.setFacilityCode("E1");
        r3.setName("Projector");
        r3.setCapacity(1);
        r3.setBuilding("Store");
        Facility f3 = new FacilityFactory().createFacility(r3);
        assertTrue(f3 instanceof Equipment);
    }

    @Test
    public void searchFacilities_callsRepository_with_null_spec_when_no_filters() {
        when(facilityRepository.findAll()).thenReturn(List.of());
        List<Facility> res = facilityService.searchFacilities(null, null, null, null, null);
        assertNotNull(res);
        verify(facilityRepository).findAll();
    }

    @Test
    public void searchFacilities_callsRepository_with_spec_when_filters_present() {
        when(facilityRepository.findAll(any(Specification.class))).thenReturn(List.of(new LectureHall("F2", "H2", 50, "B1")));
        List<Facility> res = facilityService.searchFacilities(true, Facility.FacilityType.LECTURE_HALL, 10, null, null);
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(facilityRepository).findAll(any(Specification.class));
    }
}
