package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.dto.facility.HeatmapCellDTO;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for generating facility utilization heatmaps
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FacilityHeatmapService {
    
    private final UtilizationSnapshotRepository utilizationSnapshotRepository;
    
    private static final String[] DAY_NAMES = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    
    public static final String ALL_CAMPUS_ID_STR = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";

    /**
     * Get weekly heatmap data for a facility
     * Returns 168 cells (7 days × 24 hours)
     */
    public List<HeatmapCellDTO> getWeeklyHeatmap(String facilityId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting weekly heatmap for facility: {}, dates: {} to {}", facilityId, startDate, endDate);
        
        // Get heatmap data from database
        Map<String, Integer> heatmapData;
        if (ALL_CAMPUS_ID_STR.equals(facilityId)) {
            heatmapData = utilizationSnapshotRepository.getCampusWeeklyHeatmapData(startDate, endDate);
        } else {
            heatmapData = utilizationSnapshotRepository.getWeeklyHeatmapData(facilityId, startDate, endDate);
        }
        
        List<HeatmapCellDTO> cells = new ArrayList<>();
        
        // Generate all 168 cells (7 days × 24 hours)
        for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
            for (int hour = 0; hour < 24; hour++) {
                String key = dayOfWeek + "_" + hour;
                Integer utilization = heatmapData.getOrDefault(key, 0);
                
                HeatmapCellDTO cell = HeatmapCellDTO.builder()
                    .dayOfWeek(dayOfWeek)
                    .dayName(DAY_NAMES[dayOfWeek])
                    .hour(hour)
                    .timeLabel(String.format("%02d:00", hour))
                    .avgUtilizationPercent(utilization)
                    .dataPoints(1)
                    .status(getStatusFromUtilization(utilization))
                    .color(getColorFromUtilization(utilization))
                    .build();
                
                cells.add(cell);
            }
        }
        
        return cells;
    }
    
    /**
     * Get heatmap data for a single day (24 hours)
     */
    public List<HeatmapCellDTO> getDailyHeatmap(String facilityId, LocalDate date) {
        log.debug("Getting daily heatmap for facility: {}, date: {}", facilityId, date);
        
        Map<String, Integer> heatmapData = utilizationSnapshotRepository
            .getDailyHeatmapData(facilityId, date);
        
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        String dayName = DAY_NAMES[dayOfWeek];
        
        List<HeatmapCellDTO> cells = new ArrayList<>();
        
        for (int hour = 0; hour < 24; hour++) {
            String key = hour + "";
            Integer utilization = heatmapData.getOrDefault(key, 0);
            
            HeatmapCellDTO cell = HeatmapCellDTO.builder()
                .dayOfWeek(dayOfWeek)
                .dayName(dayName)
                .hour(hour)
                .timeLabel(String.format("%02d:00", hour))
                .avgUtilizationPercent(utilization)
                .dataPoints(1)
                .status(getStatusFromUtilization(utilization))
                .color(getColorFromUtilization(utilization))
                .build();
            
            cells.add(cell);
        }
        
        return cells;
    }
    
    /**
     * Get heatmap data grouped by facility type for comparison
     */
    public Map<String, List<HeatmapCellDTO>> getHeatmapByFacilityType(
            String facilityType, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting heatmap by facility type: {}", facilityType);
        
        // This would need facilities by type query
        // Implementation depends on repository method availability
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    /**
     * Determine status from utilization percentage
     */
    private String getStatusFromUtilization(Integer utilization) {
        if (utilization == null) {
            return "UNKNOWN";
        }
        
        if (utilization < 30) {
            return "FREE";
        } else if (utilization < 70) {
            return "MODERATE";
        } else {
            return "BUSY";
        }
    }
    
    /**
     * Get color code (hex) from utilization percentage
     * Green (free) -> Yellow (moderate) -> Red (busy)
     */
    private String getColorFromUtilization(Integer utilization) {
        if (utilization == null) {
            return "#CCCCCC"; // Gray for unknown
        }
        
        // Green (0-30%)
        if (utilization < 30) {
            return "#10B981"; // Emerald green
        }
        // Yellow (30-70%)
        else if (utilization < 70) {
            return "#F59E0B"; // Amber yellow
        }
        // Red (70%+)
        else {
            return "#EF4444"; // Red
        }
    }
}
