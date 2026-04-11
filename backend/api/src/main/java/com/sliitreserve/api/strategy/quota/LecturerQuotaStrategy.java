package com.sliitreserve.api.strategy.quota;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * Lecturer quota strategy implementation.
 * 
 * Enforces moderate booking policies for lecturer and faculty users.
 * More permissive than students but less than admins.
 * 
 * Lecturer policies:
 * - Weekly quota: 99 bookings max (effectively unlimited in practice)
 * - Monthly quota: 999 bookings max (effectively unlimited in practice)
 * - Advance booking window: 90 days (3 months, same as students per FR-013)
 * - Peak hours (08:00-10:00): Allowed (per FR-019 restriction applies only to USER)
 * - High-capacity approval: May be required depending on facility configuration
 * - Permissiveness priority: 2 (moderate)
 * 
 * Design pattern: Strategy pattern as per AR-004
 * Used by: QuotaPolicyEngine and RolePolicyResolver in US3
 * 
 * Note: Lecturer bookings are auto-approved unless additional rule-based sign-off 
 * is required (per FR-016).
 */
@Component
public class LecturerQuotaStrategy extends AbstractQuotaStrategy {

    /**
     * Weekly quota limit for lecturer bookings.
     * Set to 99 as a practical unlimited value.
     * Lecturers have significantly higher quota than students.
     */
    private static final int WEEKLY_QUOTA = 99;

    /**
     * Monthly quota limit for lecturer bookings.
     * Set to 999 as a practical unlimited value.
     * Lecturers have significantly higher quota than students.
     */
    private static final int MONTHLY_QUOTA = 999;

    /**
     * Maximum advance booking window in days.
     * Lecturers can book up to 90 days (3 months) in advance, same as students (per FR-013).
     */
    private static final int MAX_ADVANCE_BOOKING_DAYS = 90;

    /**
     * Permissiveness priority level.
     * Higher than Student (1) but lower than Admin (3).
     * Used by RolePolicyResolver to determine most permissive role.
     */
    private static final int PERMISSIVENESS_PRIORITY = 2;

    @Override
    public String getRoleName() {
        return "LECTURER";
    }

    @Override
    public boolean canBookDuringPeakHours(LocalTime proposedTime) {
        // Lecturers CAN book during peak hours (peak hour restriction applies only to USER per FR-019)
        return true;
    }

    @Override
    public int getWeeklyQuota() {
        return WEEKLY_QUOTA;
    }

    @Override
    public int getMonthlyQuota() {
        return MONTHLY_QUOTA;
    }

    @Override
    public int getMaxAdvanceBookingDays() {
        return MAX_ADVANCE_BOOKING_DAYS;
    }

    @Override
    public boolean canBookWithinAdvanceWindow(LocalDate proposedDate) {
        return isWithinAdvanceWindow(proposedDate, MAX_ADVANCE_BOOKING_DAYS);
    }

    @Override
    public boolean requiresHighCapacityApproval(int facilityCapacity, int highCapacityThreshold) {
        // Lecturer booking may require high-capacity facility approval
        // if facility exceeds configured threshold (per FR-017)
        return facilityCapacity > highCapacityThreshold;
    }

    @Override
    public int getPermissivenessPriority() {
        return PERMISSIVENESS_PRIORITY;
    }
}
