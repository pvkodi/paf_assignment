package com.sliitreserve.api.unit.facility;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.LectureHall;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @InjectMocks
    private FacilityService facilityService;

    @Test
    public void searchFacilities_returnsResults() {
        Facility f = new LectureHall("F001", "Main Hall", 200, "Block A");
        when(facilityRepository.findAll(any(Specification.class))).thenReturn(List.of(f));

        List<Facility> res = facilityService.searchFacilities(true, null, null, null, null);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("F001", res.get(0).getFacilityCode());
    }
}
