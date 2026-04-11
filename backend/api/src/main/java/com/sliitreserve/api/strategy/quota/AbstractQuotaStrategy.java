package com.sliitreserve.api.strategy.quota;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Abstract base class for quota strategy implementations.
 * Provides common utility methods for policy enforcement.
 * 
 * Subclasses define:
 * - Specific quota limits (weekly, monthly, advance booking window)
 * - Peak-hour policies
 * - High-capacity approval requirements
 * - Permissiveness priority level
 * 
 * Concrete implementations (T053):
 * - StudentQuotaStrategy
 * - LecturerQuotaStrategy
 * - AdminQuotaStrategy
 */
public abstract class AbstractQuotaStrategy implements QuotaStrategy {

    /**
     * Campus local timezone for all time-based calculations.
     * Configured via application properties in T018 (TimePolicyConfig).
     * Default: Asia/Colombo (or configured in properties)
     */
    protected static final ZoneId CAMPUS_TIMEZONE = ZoneId.of("Asia/Colombo");

    /**
     * Peak hour start time (08:00) in campus local timezone.
     */
    protected static final LocalTime PEAK_HOUR_START = LocalTime.of(8, 0);

    /**
     * Peak hour end time (10:00) in campus local timezone.
     */
    protected static final LocalTime PEAK_HOUR_END = LocalTime.of(10, 0);

    /**
     * Check if a given time falls within peak hours.
     * Helper method for canBookDuringPeakHours() implementation.
     * 
     * @param time Time to check
     * @return true if time is between 08:00 and 10:00 (exclusive of end), false otherwise
     */
    protected boolean isWithinPeakHours(LocalTime time) {
        if (time == null) {
            return false;
        }
        // Peak hours: 08:00 <= time < 10:00
        return !time.isBefore(PEAK_HOUR_START) && time.isBefore(PEAK_HOUR_END);
    }

    /**
     * Check if a proposed date is within the advance booking window.
     * Helper method for canBookWithinAdvanceWindow() implementation.
     * 
     * @param proposedDate Date of proposed booking
     * @param maxAdvanceDays Maximum allowed days in advance
     * @return true if proposedDate is within window, false if beyond limit
     */
    protected boolean isWithinAdvanceWindow(LocalDate proposedDate, int maxAdvanceDays) {
        if (proposedDate == null) {
            return false;
        }
        
        // Get today's date in campus timezone
        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);
        LocalDate maxAllowedDate = today.plusDays(maxAdvanceDays);
        
        // Booking must be within [today, today + maxAdvanceDays]
        return !proposedDate.isBefore(today) && !proposedDate.isAfter(maxAllowedDate);
    }

    /**
     * Get current campus local datetime.
     * Used for time-based comparisons and logging.
     * 
     * @return Current time in campus timezone
     */
    protected ZonedDateTime now() {
        return ZonedDateTime.now(CAMPUS_TIMEZONE);
    }

    /**
     * Get today's date in campus local timezone.
     * Used for advance window calculations.
     * 
     * @return Today's date in campus timezone
     */
    protected LocalDate today() {
        return LocalDate.now(CAMPUS_TIMEZONE);
    }
}
