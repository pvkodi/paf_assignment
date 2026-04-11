package com.sliitreserve.api.unit.facility;

import com.sliitreserve.api.entities.facility.*;
import com.sliitreserve.api.factories.FacilityFactory;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.services.facility.FacilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @InjectMocks
    private FacilityService facilityService;

    @Test
    public void factory_creates_correct_subclasses() {
        Facility f1 = FacilityFactory.create(Facility.FacilityType.LECTURE_HALL, "L1", "Hall", 100, "B1");
        assertTrue(f1 instanceof LectureHall);

        Facility f2 = FacilityFactory.create(Facility.FacilityType.LAB, "LB1", "Chem Lab", 30, "B2");
        assertTrue(f2 instanceof Lab);

        Facility f3 = FacilityFactory.create(Facility.FacilityType.EQUIPMENT, "E1", "Projector", 1, "Store");
        assertTrue(f3 instanceof Equipment);
    }

    @Test
    public void searchFacilities_callsRepository_with_null_spec_when_no_filters() {
        when(facilityRepository.findAll((Specification<Facility>) null)).thenReturn(List.of());
        List<Facility> res = facilityService.searchFacilities(null, null, null, null, null);
        assertNotNull(res);
        verify(facilityRepository).findAll((Specification<Facility>) null);
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
