package com.sliitreserve.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for admin optimization opportunities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunitiesDTO {
    
    @JsonProperty("underutilized_facilities")
    private List<UnderutilizedFacilityDTO> underutilizedFacilities;
    
    @JsonProperty("over_capacity_times")
    private List<OverCapacityAlertDTO> overCapacityAlerts;
    
    @JsonProperty("consolidation_candidates")
    private List<ConsolidationPairDTO> consolidationCandidates;
    
    /**
     * Nested DTO for underutilized facilities
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnderutilizedFacilityDTO {
        @JsonProperty("facility_id")
        private String facilityId;
        
        @JsonProperty("facility_name")
        private String facilityName;
        
        @JsonProperty("facility_type")
        private String facilityType;
        
        @JsonProperty("building")
        private String building;
        
        @JsonProperty("capacity")
        private Integer capacity;
        
        @JsonProperty("avg_utilization_percent")
        private Integer avgUtilizationPercent;
        
        @JsonProperty("underutilized_days")
        private Integer underutilizedDays;
        
        @JsonProperty("consecutive_low_days")
        private Integer consecutiveLowDays;
        
        @JsonProperty("suggestions")
        private List<String> suggestions; // ["consolidate", "relocate", "repurpose"]
    }
    
    /**
     * Nested DTO for over-capacity alerts
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverCapacityAlertDTO {
        @JsonProperty("facility_id")
        private String facilityId;
        
        @JsonProperty("facility_name")
        private String facilityName;
        
        @JsonProperty("building")
        private String building;
        
        @JsonProperty("peak_time_description")
        private String peakTimeDescription; // "Tue-Thu 10am-12pm"
        
        @JsonProperty("peak_utilization_percent")
        private Integer peakUtilizationPercent;
        
        @JsonProperty("peak_occurrence_count")
        private Integer peakOccurrenceCount;
        
        @JsonProperty("suggested_overflow_facility_id")
        private String suggestedOverflowFacilityId;
        
        @JsonProperty("suggested_overflow_facility_name")
        private String suggestedOverflowFacilityName;
        
        @JsonProperty("suggestion")
        private String suggestion; // "Use Facility B as overflow"
    }
    
    /**
     * Nested DTO for consolidation candidate pairs
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConsolidationPairDTO {
        @JsonProperty("facility_a_id")
        private String facilityAId;
        
        @JsonProperty("facility_a_name")
        private String facilityAName;
        
        @JsonProperty("facility_a_capacity")
        private Integer facilityACapacity;
        
        @JsonProperty("facility_a_avg_utilization")
        private Integer facilityAAvgUtilization;
        
        @JsonProperty("facility_b_id")
        private String facilityBId;
        
        @JsonProperty("facility_b_name")
        private String facilityBName;
        
        @JsonProperty("facility_b_capacity")
        private Integer facilityBCapacity;
        
        @JsonProperty("facility_b_avg_utilization")
        private Integer facilityBAvgUtilization;
        
        @JsonProperty("similarity_score")
        private Double similarityScore; // 0-100, higher = more similar
        
        @JsonProperty("building")
        private String building;
        
        @JsonProperty("recommendation")
        private String recommendation; // "Consolidate these, keep A close to classroom"
    }
}
