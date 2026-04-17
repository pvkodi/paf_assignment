package com.sliitreserve.api.controllers.facilities;

import com.sliitreserve.api.dto.facility.AvailabilityStatusDTO;
import com.sliitreserve.api.services.facility.FacilityInsightsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sliitreserve.api.dto.ErrorResponseDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for facility insights and availability information
 */
@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
@Slf4j
public class FacilityInsightsController {
    
    private final FacilityInsightsService facilityInsightsService;
    
    @GetMapping("/{facilityId}/insights")
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getFacilityInsights(@PathVariable String facilityId) {
        try {
            log.info("Getting insights for facility: {}", facilityId);
            AvailabilityStatusDTO insights = facilityInsightsService.getFacilityInsights(facilityId);
            return ResponseEntity.ok(insights);
        } catch (IllegalArgumentException e) {
            log.warn("Facility not found: {}", facilityId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO("FACILITY_NOT_FOUND", "Facility does not exist: " + facilityId));
        } catch (Exception e) {
            log.error("Error getting facility insights for {}: {}", facilityId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "Failed to retrieve facility insights"));
        }
    }
    
    // Uses shared ErrorResponseDTO from com.sliitreserve.api.dto
}
