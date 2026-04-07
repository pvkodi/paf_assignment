package com.sliitreserve.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

/**
 * Timezone and Time Policy Configuration.
 * 
 * Establishes campus-wide timezone policies for:
 * - Booking time constraints (peak-hour windows, advance-booking limits)
 * - No-show classification (15-minute window from booking start)
 * - Recurring booking recurrence evaluation and public holiday skipping
 * - SLA deadline calculations (24x7 elapsed time)
 * - All time-sensitive business logic requiring institutional consistency
 * 
 * Design Decision (Research Decision 10):
 * - Use campus local timezone with DST support for all time operations
 * - Rationale: Matches business operations (peak hours in institutional terms)
 *   and ensures consistent institutional policy enforcement
 * - Alternative: UTC was rejected due to policy misalignment
 * - Alternative: User-local timezones rejected due to requiring per-user policy resolution
 * 
 * Requirements Supported:
 * - FR-010: Recurring bookings skip occurrences on configured public holidays
 * - FR-019: Peak-hour restriction from 08:00 to 10:00 in campus local timezone
 * - FR-021: No-show classified within 15 minutes of booking start in campus local timezone
 * - FR-032: SLA deadlines enforced using 24x7 elapsed time with DST support
 * - FR-041: Campus local timezone with DST used for all time operations
 * 
 * Integration Points:
 * - BookingService: Uses timezone for capacity, overlap, recurrence skip logic
 * - CheckInService: Uses timezone for 15-minute no-show window
 * - SlaScheduler: Uses timezone for SLA deadline calculation (4h/8h/24h/72h)
 * - PublicHolidayService: Evaluates holidays in campus timezone
 * - QuotaPolicyEngine: Uses timezone for peak-hour restriction checks
 */
@Slf4j
@Configuration
public class TimePolicyConfig {

    /**
     * Campus local timezone ID (e.g., "Asia/Colombo" for Sri Lanka campus).
     * Can be overridden via app.time-policy.campus-timezone property.
     * Default: "Asia/Colombo"
     */
    @Value("${app.time-policy.campus-timezone:Asia/Colombo}")
    private String campusTimezoneId;

    /**
     * Peak hour start time in HH:mm format (e.g., "08:00").
     * Used for peak-hour restriction policy enforcement.
     * Default: "08:00"
     */
    @Value("${app.time-policy.peak-hour-start:08:00}")
    private String peakHourStart;

    /**
     * Peak hour end time in HH:mm format (e.g., "10:00").
     * Used for peak-hour restriction policy enforcement.
     * Default: "10:00"
     */
    @Value("${app.time-policy.peak-hour-end:10:00}")
    private String peakHourEnd;

    /**
     * No-show grace period in minutes (time from booking start to classify as no-show).
     * Default: 15 minutes
     * Related to FR-021: No-show when check-in does not occur within 15 minutes
     */
    @Value("${app.time-policy.no-show-grace-minutes:15}")
    private int noShowGraceMinutes;

    /**
     * Bean providing campus timezone zone ID with validation.
     * 
     * @return ZoneId configured for campus timezone with DST support
     * @throws ZoneRulesException if timezone ID is invalid
     */
    @Bean
    public ZoneId campusZoneId() {
        log.info("Initializing campus timezone configuration");
        log.info("Campus timezone ID: {}", campusTimezoneId);

        try {
            ZoneId zoneId = ZoneId.of(campusTimezoneId);
            log.info("Campus timezone loaded: {} (DST-aware: {})", 
                zoneId, zoneId.getRules().isFixedOffset() ? "no" : "yes");
            return zoneId;
        } catch (ZoneRulesException e) {
            log.error("Invalid campus timezone ID: {}", campusTimezoneId, e);
            throw new RuntimeException("Invalid campus timezone ID: " + campusTimezoneId, e);
        }
    }

    /**
     * Bean providing time policy configuration properties.
     * Includes timezone, peak-hour windows, and no-show grace period.
     * 
     * @param campusZoneId The configured campus timezone
     * @return TimePolicyProperties with all validated configuration values
     */
    @Bean
    public TimePolicyProperties timePolicyProperties(ZoneId campusZoneId) {
        log.info("Initializing time policy configuration");
        log.info("Peak-hour window: {} to {} (campus timezone)", peakHourStart, peakHourEnd);
        log.info("No-show grace period: {} minutes", noShowGraceMinutes);

        return new TimePolicyProperties(
            campusZoneId,
            peakHourStart,
            peakHourEnd,
            noShowGraceMinutes
        );
    }

    /**
     * Container for time policy configuration properties.
     * Provides immutable access to timezone and policy settings.
     */
    public static class TimePolicyProperties {
        public final ZoneId campusZoneId;
        public final String peakHourStart;  // Format: HH:mm
        public final String peakHourEnd;    // Format: HH:mm
        public final int noShowGraceMinutes;

        public TimePolicyProperties(
            ZoneId campusZoneId,
            String peakHourStart,
            String peakHourEnd,
            int noShowGraceMinutes
        ) {
            this.campusZoneId = campusZoneId;
            this.peakHourStart = peakHourStart;
            this.peakHourEnd = peakHourEnd;
            this.noShowGraceMinutes = noShowGraceMinutes;
        }

        /**
         * Parse peak hour start time to hour component.
         * Format: "HH:mm"
         * Example: "08:00" -> 8
         */
        public int getPeakHourStartHour() {
            return Integer.parseInt(peakHourStart.split(":")[0]);
        }

        /**
         * Parse peak hour end time to hour component.
         * Format: "HH:mm"
         * Example: "10:00" -> 10
         */
        public int getPeakHourEndHour() {
            return Integer.parseInt(peakHourEnd.split(":")[0]);
        }
    }
}
