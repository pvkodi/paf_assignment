package com.sliitreserve.api.contract.facility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sliitreserve.api.controllers.FacilityController;
import com.sliitreserve.api.entities.facility.LectureHall;
import com.sliitreserve.api.services.facility.FacilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FacilityContractTest {

    private MockMvc mockMvc;
    private FacilityService facilityService;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        facilityService = Mockito.mock(FacilityService.class);
        FacilityController controller = new FacilityController();
        ReflectionTestUtils.setField(controller, "facilityService", facilityService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getFacilities_endpoint_returns_json_array() throws Exception {
        when(facilityService.searchFacilities(null, null, null, null, null))
                .thenReturn(List.of(new LectureHall("F100", "Test Hall", 100, "Block A")));

        mockMvc.perform(get("/api/facilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void getByCode_returns_404_for_missing() throws Exception {
        when(facilityService.findByCode("NOPE")).thenReturn(null);

        mockMvc.perform(get("/api/facilities/NOPE").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
