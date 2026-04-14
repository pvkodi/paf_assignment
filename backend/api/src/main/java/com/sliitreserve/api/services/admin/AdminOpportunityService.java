package com.sliitreserve.api.services.admin;

import com.sliitreserve.api.dto.admin.OpportunitiesDTO;
import com.sliitreserve.api.dto.admin.OpportunitiesDTO.ConsolidationPairDTO;
import com.sliitreserve.api.dto.admin.OpportunitiesDTO.OverCapacityAlertDTO;
import com.sliitreserve.api.dto.admin.OpportunitiesDTO.UnderutilizedFacilityDTO;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating admin optimization opportunities and alerts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminOpportunityService {
    
    private final FacilityRepository facilityRepository;
    private final UtilizationSnapshotRepository utilizationSnapshotRepository;
    
    private static final int UNDERUTILIZATION_THRESHOLD = 30; // <30%
    private static final int OVERCAPACITY_THRESHOLD = 85; // >85%
    private static final int SIMILARITY_THRESHOLD = 5; // Within 5% utilization
    
    /**
     * Get all optimization opportunities (underutilization, over-capacity, consolidation)
     */
    public OpportunitiesDTO getOptimizationOpportunities() {
        log.debug("Generating optimization opportunities report");
        
        List<Facility> allFacilities = facilityRepository.findAll();
        
        // Get underutilization alerts
        List<UnderutilizedFacilityDTO> underutilized = getUnderutilizedFacilities(allFacilities);
        
        // Get over-capacity alerts
        List<OverCapacityAlertDTO> overCapacity = getOverCapacityAlerts(allFacilities);
        
        // Get consolidation candidates
        List<ConsolidationPairDTO> consolidationPairs = getConsolidationCandidates(allFacilities);
        
        return OpportunitiesDTO.builder()
            .underutilizedFacilities(underutilized)
            .overCapacityAlerts(overCapacity)
            .consolidationCandidates(consolidationPairs)
            .build();
    }
    
    /**
     * Get facilities with utilization below threshold
     */
    private List<UnderutilizedFacilityDTO> getUnderutilizedFacilities(List<Facility> facilities) {
        
        return facilities.stream()
            .map(facility -> {
                String facilityId = facility.getId().toString();
                Integer avgUtilization = utilizationSnapshotRepository
                    .getAverageUtilization(facilityId, 30);
                
                if (avgUtilization != null && avgUtilization < UNDERUTILIZATION_THRESHOLD) {
                    Integer underutilizedDays = utilizationSnapshotRepository
                        .countUnderutilizedDays(facilityId, UNDERUTILIZATION_THRESHOLD, 30);
                    
                    Integer consecutiveRuns = utilizationSnapshotRepository
                        .findConsecutiveUnderutilizedDays(facilityId, UNDERUTILIZATION_THRESHOLD);
                    
                    List<String> suggestions = generateUnderutilizationSuggestions(
                        facility.getType() != null ? facility.getType().name() : "UNKNOWN",
                        avgUtilization
                    );
                    
                    return UnderutilizedFacilityDTO.builder()
                        .facilityId(facilityId)
                        .facilityName(facility.getName())
                        .facilityType(facility.getType() != null ? facility.getType().name() : "UNKNOWN")
                        .building(facility.getBuilding())
                        .capacity(facility.getCapacity())
                        .avgUtilizationPercent(avgUtilization)
                        .underutilizedDays(underutilizedDays != null ? underutilizedDays : 0)
                        .consecutiveLowDays(consecutiveRuns != null ? consecutiveRuns : 0)
                        .suggestions(suggestions)
                        .build();
                }
                
                return null;
            })
            .filter(x -> x != null)
            .sorted((a, b) -> Integer.compare(a.getAvgUtilizationPercent(), b.getAvgUtilizationPercent()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get time periods with over-capacity situations
     */
    private List<OverCapacityAlertDTO> getOverCapacityAlerts(List<Facility> facilities) {
        List<OverCapacityAlertDTO> alerts = new ArrayList<>();
        
        // Get facilities grouped by type for overflow suggestions
        Map<String, List<Facility>> facilitiesByType = facilities.stream()
            .collect(Collectors.groupingBy(f -> f.getType() != null ? f.getType().name() : "UNKNOWN"));
        
        for (Facility facility : facilities) {
            String facilityId = facility.getId().toString();
            var peakTimes = utilizationSnapshotRepository
                .findPeakUtilizationTimeslots(facilityId, OVERCAPACITY_THRESHOLD);
            
            for (var peak : peakTimes.entrySet()) {
                String[] parts = peak.getKey().split("_");
                int dayOfWeek = Integer.parseInt(parts[0]);
                int hour = Integer.parseInt(parts[1]);
                int utilization = peak.getValue();
                
                // Find similar facility in same building that could handle overflow
                String facilityTypeStr = facility.getType() != null ? facility.getType().name() : "UNKNOWN";
                Facility overflowFacility = findOverflowFacility(
                    facility,
                    facilitiesByType.get(facilityTypeStr)
                );
                
                OverCapacityAlertDTO alert = OverCapacityAlertDTO.builder()
                    .facilityId(facilityId)
                    .facilityName(facility.getName())
                    .building(facility.getBuilding())
                    .peakTimeDescription(formatPeakTimeDescription(dayOfWeek, hour))
                    .peakUtilizationPercent(utilization)
                    .peakOccurrenceCount(countOccurrences(dayOfWeek, hour, facilityId))
                    .suggestedOverflowFacilityId(overflowFacility != null ? overflowFacility.getId().toString() : null)
                    .suggestedOverflowFacilityName(overflowFacility != null ? overflowFacility.getName() : null)
                    .suggestion(overflowFacility != null 
                        ? "Use " + overflowFacility.getName() + " as overflow"
                        : "Add capacity or adjust bookings")
                    .build();
                
                alerts.add(alert);
            }
        }
        
        return alerts;
    }
    
    /**
     * Get consolidation candidates (similar facilities that could be merged)
     */
    private List<ConsolidationPairDTO> getConsolidationCandidates(List<Facility> facilities) {
        List<ConsolidationPairDTO> pairs = new ArrayList<>();
        
        // Group by facility type
        Map<String, List<Facility>> facilitiesByType = facilities.stream()
            .collect(Collectors.groupingBy(f -> f.getType() != null ? f.getType().name() : "UNKNOWN"));
        
        for (List<Facility> typeFacilities : facilitiesByType.values()) {
            // Compare within same type
            for (int i = 0; i < typeFacilities.size(); i++) {
                for (int j = i + 1; j < typeFacilities.size(); j++) {
                    Facility facilityA = typeFacilities.get(i);
                    Facility facilityB = typeFacilities.get(j);
                    
                    // Only suggest consolidation if in same building
                    if (!facilityA.getBuilding().equals(facilityB.getBuilding())) {
                        continue;
                    }
                    
                    String idA = facilityA.getId().toString();
                    String idB = facilityB.getId().toString();
                    Integer utilA = utilizationSnapshotRepository
                        .getAverageUtilization(idA, 30);
                    Integer utilB = utilizationSnapshotRepository
                        .getAverageUtilization(idB, 30);
                    
                    if (utilA != null && utilB != null) {
                        double difference = Math.abs(utilA - utilB);
                        
                        if (difference < SIMILARITY_THRESHOLD &&
                            Math.abs(facilityA.getCapacity() - facilityB.getCapacity()) <= 5) {
                            
                            ConsolidationPairDTO pair = ConsolidationPairDTO.builder()
                                .facilityAId(idA)
                                .facilityAName(facilityA.getName())
                                .facilityACapacity(facilityA.getCapacity())
                                .facilityAAvgUtilization(utilA)
                                .facilityBId(idB)
                                .facilityBName(facilityB.getName())
                                .facilityBCapacity(facilityB.getCapacity())
                                .facilityBAvgUtilization(utilB)
                                .similarityScore(100.0 - difference)
                                .building(facilityA.getBuilding())
                                .recommendation(String.format(
                                    "Consolidate %s and %s into one. Keep %s for accessibility.",
                                    facilityA.getName(), facilityB.getName(), facilityA.getName()))
                                .build();
                            
                            pairs.add(pair);
                        }
                    }
                }
            }
        }
        
        // Sort by similarity score (highest first)
        return pairs.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
            .collect(Collectors.toList());
    }
    
    /**
     * Generate suggestions based on utilization level
     */
    private List<String> generateUnderutilizationSuggestions(String facilityType, Integer utilization) {
        List<String> suggestions = new ArrayList<>();
        
        if (utilization < 15) {
            suggestions.add("Consider relocating resources");
            suggestions.add("Consolidate with similar facility");
            suggestions.add("Repurpose for different use");
        } else {
            suggestions.add("Monitor usage patterns");
            suggestions.add("Consolidate if similar facility exists");
        }
        
        return suggestions;
    }
    
    /**
     * Find facility in same type/building for overflow
     */
    private Facility findOverflowFacility(Facility primary, List<Facility> candidates) {
        return candidates.stream()
            .filter(f -> !f.getId().equals(primary.getId()) && f.getBuilding().equals(primary.getBuilding()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Format peak time description
     */
    private String formatPeakTimeDescription(int dayOfWeek, int hour) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return String.format("%s %02d:00-%02d:00", days[dayOfWeek], hour, hour + 1);
    }
    
    /**
     * Count how many times a peak occurs
     */
    private Integer countOccurrences(int dayOfWeek, int hour, String facilityId) {
        // This would count how many times this specific DOW/hour combination exceeds threshold
        return 5; // Placeholder
    }
}
