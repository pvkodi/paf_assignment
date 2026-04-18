package com.sliitreserve.api.entities.facility;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Embeddable availability window for a facility.
 * Stored in the facility_availability_windows join table.
 * Supports multiple windows per day and full Mon–Sun coverage.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityWindow {

    @NotNull(message = "Day of week is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Returns true if the given day and time falls within this window.
     * Inclusive of startTime, exclusive of endTime.
     */
    public boolean contains(DayOfWeek day, LocalTime time) {
        if (day == null || time == null || dayOfWeek == null) return false;
        return dayOfWeek == day
                && !time.isBefore(startTime)
                && time.isBefore(endTime);
    }

    /**
     * Returns true if this window overlaps with another window.
     * Used for validation.
     */
    public boolean overlapsWith(AvailabilityWindow other) {
        if (other == null || this.dayOfWeek != other.dayOfWeek) return false;
        return this.startTime.isBefore(other.endTime)
                && other.startTime.isBefore(this.endTime);
    }
}
