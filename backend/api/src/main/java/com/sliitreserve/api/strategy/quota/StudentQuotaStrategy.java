package com.sliitreserve.api.strategy.quota;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * User (Student) quota strategy implementation.
 * 
 * Enforces the most restrictive booking policies for regular user (student) roles.
 * 
 * User policies:
 * - Weekly quota: 3 bookings max
 * - Monthly quota: 10 bookings max
 * - Advance booking window: 90 days (3 months)
 * - Peak hours (08:00-10:00): NOT allowed
 * - High-capacity approval: NOT required for users
 * - Permissiveness priority: 1 (most restrictive)
 * 
 * Design pattern: Strategy pattern as per AR-004
 * Used by: QuotaPolicyEngine and RolePolicyResolver in US3
 */
@Component
public class StudentQuotaStrategy extends AbstractQuotaStrategy {

    /**
     * Weekly quota limit for student bookings.
     * Maximum 3 bookings per calendar week (Monday-Sunday).
     */
    private static final int WEEKLY_QUOTA = 3;

    /**
     * Monthly quota limit for student bookings.
     * Maximum 10 bookings per calendar month.
     */
    private static final int MONTHLY_QUOTA = 10;

    /**
     * Maximum advance booking window in days.
     * Students can book up to 90 days (3 months) in advance.
     */
    private static final int MAX_ADVANCE_BOOKING_DAYS = 90;

    /**
     * Permissiveness priority level.
     * Lower priority indicates more restrictive policy.
     * Used by RolePolicyResolver to determine most permissive role.
     */
    private static final int PERMISSIVENESS_PRIORITY = 1;

    @Override
    public String getRoleName() {
        return "USER";
    }

    @Override
    public boolean canBookDuringPeakHours(LocalTime proposedTime) {
        // Students CANNOT book during peak hours (08:00-10:00)
        // Return false if time is within peak hours, true otherwise
        return !isWithinPeakHours(proposedTime);
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
        // Students do NOT require high-capacity facility approval
        return false;
    }

    @Override
    public int getPermissivenessPriority() {
        return PERMISSIVENESS_PRIORITY;
    }
}
