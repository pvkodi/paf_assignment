package com.sliitreserve.api.dto.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for weekly heatmap cell (single hour/day combination)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatmapCellDTO {
    
    /**
     * Day of week: 0 = Sunday, 1 = Monday, ..., 6 = Saturday
     */
    @JsonProperty("day_of_week")
    private Integer dayOfWeek;
    
    @JsonProperty("day_name")
    private String dayName;
    
    /**
     * Hour of day: 0-23
     */
    @JsonProperty("hour")
    private Integer hour;
    
    @JsonProperty("time_label")
    private String timeLabel; // "10:00"
    
    /**
     * Average utilization percentage for this slot (0-100)
     */
    @JsonProperty("avg_utilization_percent")
    private Integer avgUtilizationPercent;
    
    /**
     * Number of data points used to calculate average
     */
    @JsonProperty("data_points")
    private Integer dataPoints;
    
    /**
     * Availability status: "FREE" (<30%), "MODERATE" (30-70%), "BUSY" (>70%)
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * RGB color for heatmap visualization: "#00FF00" for green, etc.
     */
    @JsonProperty("color")
    private String color;
}
