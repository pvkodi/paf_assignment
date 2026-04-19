package com.sliitreserve.api.controllers.admin;

import com.sliitreserve.api.dto.admin.OpportunitiesDTO;
import com.sliitreserve.api.services.admin.AdminOpportunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sliitreserve.api.dto.ErrorResponseDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for admin facility optimization opportunities
 * Restricted to FACILITY_MANAGER and ADMIN roles only
 */
@RestController
@RequestMapping("/api/v1/admin/facilities")
@RequiredArgsConstructor
@Slf4j
public class AdminOpportunitiesController {
    
    private final AdminOpportunityService adminOpportunityService;
    
    /**
     * Get all optimization opportunities and alerts for facility management
     *
     * Includes:
     * - Underutilized facilities (< 30% utilization)
     * - Over-capacity times (> 85% utilization)
     * - Consolidation candidates (similar facilities in same building)
     *
     * @return OpportunitiesDTO with comprehensive optimization report
     */
    @GetMapping("/optimization-opportunities")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getOptimizationOpportunities() {
        try {
            log.info("Generating optimization opportunities report");
            
            OpportunitiesDTO opportunities = adminOpportunityService.getOptimizationOpportunities();
            
            return ResponseEntity.ok(opportunities);
        } catch (Exception e) {
            log.error("Error generating optimization opportunities report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "Failed to generate optimization report"));
        }
    }
    
    // Uses shared ErrorResponseDTO from com.sliitreserve.api.dto
}
