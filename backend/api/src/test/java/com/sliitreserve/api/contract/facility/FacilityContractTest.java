package com.sliitreserve.api.contract.facility;

import com.sliitreserve.api.controllers.facilities.FacilityController;
import com.sliitreserve.api.controllers.advice.GlobalExceptionHandler;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.services.facility.FacilityOptimizationService;
import com.sliitreserve.api.services.facility.FacilityTimetableService;
import com.sliitreserve.api.services.facility.FacilityService;
import com.sliitreserve.api.services.facility.TimetableParserService;
import com.sliitreserve.api.services.facility.FacilityRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FacilityContractTest {

    private MockMvc mockMvc;
    private FacilityService facilityService;
    private FacilityOptimizationService facilityOptimizationService;
    private FacilityTimetableService facilityTimetableService;
    private TimetableParserService timetableParserService;
    private FacilityRuleEngine facilityRuleEngine;
    private FacilityRepository facilityRepository;
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        facilityService = Mockito.mock(FacilityService.class);
        facilityOptimizationService = Mockito.mock(FacilityOptimizationService.class);
        facilityTimetableService = Mockito.mock(FacilityTimetableService.class);
        timetableParserService = Mockito.mock(TimetableParserService.class);
        facilityRuleEngine = Mockito.mock(FacilityRuleEngine.class);
        facilityRepository = Mockito.mock(FacilityRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        FacilityController controller = new FacilityController(
                facilityService, facilityOptimizationService,
                facilityTimetableService, timetableParserService,
                facilityRuleEngine, facilityRepository, userRepository);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void getFacilities_endpoint_returns_paged_json() throws Exception {
        FacilityResponseDTO dto = new FacilityResponseDTO();
        dto.setId(UUID.randomUUID());
        dto.setName("Test Hall");
        dto.setType(Facility.FacilityType.LECTURE_HALL);

        when(facilityService.listFacilities(any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/facilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].name").value("Test Hall"));
    }

    @Test
    public void getById_returns_404_for_missing() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(facilityService.getFacilityById(missingId)).thenThrow(new ResourceNotFoundException("Facility", missingId.toString()));

        mockMvc.perform(get("/api/facilities/" + missingId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void searchFacilities_endpoint_returns_filtered_paged_json() throws Exception {
        FacilityResponseDTO dto = new FacilityResponseDTO();
        dto.setId(UUID.randomUUID());
        dto.setName("C-LAB-01");
        dto.setType(Facility.FacilityType.LAB);
        dto.setBuilding("C");
        dto.setStatus(Facility.FacilityStatus.ACTIVE);
        dto.setAvailabilityStartTime(LocalTime.of(8, 0));
        dto.setAvailabilityEndTime(LocalTime.of(18, 0));

        when(facilityService.searchFacilities(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/v1/facilities/search")
                        .param("query", "lab c")
                        .param("type", "LAB")
                        .param("building", "C")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].name").value("C-LAB-01"))
                .andExpect(jsonPath("$.content[0].type").value("LAB"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
