package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.dto.facility.HeatmapCellDTO;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating facility utilization heatmaps
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FacilityHeatmapService {
    
    private final UtilizationSnapshotRepository utilizationSnapshotRepository;
    
    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    
    public static final String ALL_CAMPUS_ID_STR = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";

    /**
     * Get weekly heatmap data for a facility
     * Returns 168 cells (7 days × 24 hours)
     */
    public List<HeatmapCellDTO> getWeeklyHeatmap(String facilityId, LocalDate startDate, LocalDate endDate) {
        log.debug("Getting weekly heatmap for facility: {}, dates: {} to {}", facilityId, startDate, endDate);

        List<UtilizationSnapshot> snapshots;
        if (ALL_CAMPUS_ID_STR.equals(facilityId)) {
            snapshots = utilizationSnapshotRepository.findBySnapshotDateBetweenOrderBySnapshotDateAsc(startDate, endDate);
        } else {
            snapshots = utilizationSnapshotRepository.findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                UUID.fromString(facilityId),
                startDate,
                endDate
            );
        }

        Map<String, HeatmapAggregate> aggregates = new HashMap<>();
        for (UtilizationSnapshot snapshot : snapshots) {
            Facility facility = snapshot.getFacility();
            if (facility == null || snapshot.getSnapshotDate() == null) {
                continue;
            }

            int dayOfWeek = snapshot.getSnapshotDate().getDayOfWeek().getValue() - 1;
            int utilization = snapshot.getUtilizationPercent() != null
                ? (int) Math.round(snapshot.getUtilizationPercent().doubleValue())
                : 0;
            int addedHours = 0;

            for (int hour = 0; hour < 24; hour++) {
                LocalTime slot = LocalTime.of(hour, 0);
                if (!facility.isAvailableAt(snapshot.getSnapshotDate().getDayOfWeek(), slot)) {
                    continue;
                }

                String key = dayOfWeek + "_" + hour;
                aggregates.computeIfAbsent(key, k -> new HeatmapAggregate()).add(utilization);
                addedHours++;
            }

            if (addedHours == 0) {
                int fallbackHour = resolveFallbackHour(facility);
                String key = dayOfWeek + "_" + fallbackHour;
                aggregates.computeIfAbsent(key, k -> new HeatmapAggregate()).add(utilization);
            }
        }
        
        List<HeatmapCellDTO> cells = new ArrayList<>();
        
        // Generate all 168 cells (7 days × 24 hours)
        for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
            for (int hour = 0; hour < 24; hour++) {
                String key = dayOfWeek + "_" + hour;
                HeatmapAggregate aggregate = aggregates.get(key);
                Integer utilization = aggregate != null ? aggregate.average() : 0;
                
                HeatmapCellDTO cell = HeatmapCellDTO.builder()
                    .dayOfWeek(dayOfWeek)
                    .dayName(DAY_NAMES[dayOfWeek])
                    .hour(hour)
                    .timeLabel(String.format("%02d:00", hour))
                    .avgUtilizationPercent(utilization)
                    .dataPoints(aggregate != null ? aggregate.count() : 0)
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

        int dayOfWeek = date.getDayOfWeek().getValue() - 1;
        return getWeeklyHeatmap(facilityId, date, date).stream()
            .filter(cell -> cell.getDayOfWeek() != null && cell.getDayOfWeek() == dayOfWeek)
            .toList();
    }

    private int resolveFallbackHour(Facility facility) {
        if (facility.getAvailabilityStart() != null) {
            return facility.getAvailabilityStart().getHour();
        }
        return 12;
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

    private static final class HeatmapAggregate {
        private int total;
        private int count;

        void add(int utilization) {
            total += utilization;
            count++;
        }

        int average() {
            if (count == 0) {
                return 0;
            }
            return Math.round((float) total / count);
        }

        int count() {
            return count;
        }
    }
}
