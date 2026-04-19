package com.sliitreserve.api.controllers.facilities;

import com.sliitreserve.api.dto.facility.HeatmapCellDTO;
import com.sliitreserve.api.services.facility.FacilityHeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.sliitreserve.api.dto.ErrorResponseDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
@Slf4j
public class FacilityHeatmapController {
    
    private final FacilityHeatmapService facilityHeatmapService;
    
    @GetMapping("/{facilityId}/heatmap")
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getWeeklyHeatmap(
            @PathVariable String facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            LocalDate start = startDate != null ? startDate : end.minusDays(30);
            
            log.info("Getting heatmap for facility: {} from {} to {}", facilityId, start, end);
            
            List<HeatmapCellDTO> heatmapData = facilityHeatmapService.getWeeklyHeatmap(facilityId, start, end);
            
            return ResponseEntity.ok(new HeatmapResponse(facilityId, start, end, heatmapData));
        } catch (Exception e) {
            log.error("Error getting heatmap for facility {}: {}", facilityId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "Failed to retrieve heatmap data"));
        }
    }
    
    @GetMapping("/{facilityId}/heatmap/daily")
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getDailyHeatmap(
            @PathVariable String facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            log.info("Getting daily heatmap for facility: {} on {}", facilityId, date);
            
            List<HeatmapCellDTO> heatmapData = facilityHeatmapService.getDailyHeatmap(facilityId, date);
            
            return ResponseEntity.ok(new HeatmapResponse(facilityId, date, date, heatmapData));
        } catch (Exception e) {
            log.error("Error getting daily heatmap for facility {}: {}", facilityId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "Failed to retrieve daily heatmap data"));
        }
    }
    
    public record HeatmapResponse(
            String facilityId,
            LocalDate startDate,
            LocalDate endDate,
            List<HeatmapCellDTO> cells
    ) {}
    
    // Uses shared ErrorResponseDTO from com.sliitreserve.api.dto
}
