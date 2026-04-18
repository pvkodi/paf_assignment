package com.sliitreserve.api.dto.facility;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO for a single availability window (a day + start/end time pair).
 * A facility may have multiple windows — including multiple windows on the same day.
 *
 * Example:
 *   { dayOfWeek: "MONDAY", startTime: "08:00", endTime: "12:00" }
 *   { dayOfWeek: "MONDAY", startTime: "14:00", endTime: "18:00" }
 *   { dayOfWeek: "SATURDAY", startTime: "10:00", endTime: "14:00" }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityWindowDTO {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;
}
