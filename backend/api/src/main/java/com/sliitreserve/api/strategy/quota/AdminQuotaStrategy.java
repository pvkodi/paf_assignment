package com.sliitreserve.api.strategy.quota;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * Admin quota strategy implementation.
 * 
 * Enforces the least restrictive booking policies for administrative users.
 * Admins have maximum flexibility for booking operations, including on behalf of users (FR-012).
 * 
 * Admin policies:
 * - Weekly quota: 9999 bookings max (unlimited for practical purposes)
 * - Monthly quota: 9999 bookings max (unlimited for practical purposes)
 * - Advance booking window: 180 days (6 months, double that of users/lecturers per FR-013)
 * - Peak hours (08:00-10:00): Allowed without restriction
 * - High-capacity approval: NOT required (admins bypass approval workflows)
 * - Permissiveness priority: 3 (most permissive)
 * 
 * Design pattern: Strategy pattern as per AR-004
 * Used by: QuotaPolicyEngine and RolePolicyResolver in US3
 * 
 * Note: Admin bookings bypass the typical approval chain and can include bookings
 * on behalf of other users (per FR-012). High-capacity facilities do not require
 * additional sign-off for admin bookings.
 */
@Component
public class AdminQuotaStrategy extends AbstractQuotaStrategy {

    /**
     * Weekly quota limit for admin bookings.
     * Set to 9999 as a practical unlimited value.
     * Admins have no meaningful quota restriction.
     */
    private static final int WEEKLY_QUOTA = 9999;

    /**
     * Monthly quota limit for admin bookings.
     * Set to 9999 as a practical unlimited value.
     * Admins have no meaningful quota restriction.
     */
    private static final int MONTHLY_QUOTA = 9999;

    /**
     * Maximum advance booking window in days.
     * Admins can book up to 180 days (6 months) in advance (per FR-013),
     * double the window allowed for users and lecturers.
     */
    private static final int MAX_ADVANCE_BOOKING_DAYS = 180;

    /**
     * Permissiveness priority level.
     * Highest priority, indicating most permissive policy.
     * Used by RolePolicyResolver to determine most permissive role.
     */
    private static final int PERMISSIVENESS_PRIORITY = 3;

    @Override
    public String getRoleName() {
        return "ADMIN";
    }

    @Override
    public boolean canBookDuringPeakHours(LocalTime proposedTime) {
        // Admins CAN book during peak hours without restriction
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
        // Admins do NOT require high-capacity facility approval
        // Admins bypass the typical approval workflows
        return false;
    }

    @Override
    public int getPermissivenessPriority() {
        return PERMISSIVENESS_PRIORITY;
    }
}
