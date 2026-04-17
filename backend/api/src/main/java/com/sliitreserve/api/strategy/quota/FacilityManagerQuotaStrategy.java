package com.sliitreserve.api.strategy.quota;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * Facility Manager quota strategy implementation.
 * 
 * Enforces permissive booking policies for facility management users.
 * Facility managers need high quota allowances to manage their facilities effectively,
 * including the ability to book on behalf of other users (FR-012).
 * 
 * Facility Manager policies:
 * - Weekly quota: 9999 bookings max (unlimited for practical purposes)
 * - Monthly quota: 9999 bookings max (unlimited for practical purposes)
 * - Advance booking window: 180 days (6 months, same as admin)
 * - Peak hours (08:00-10:00): Allowed without restriction
 * - High-capacity approval: NOT required (facility managers bypass approval for their facilities)
 * - Permissiveness priority: 3 (tied with ADMIN, highly permissive)
 * 
 * Design pattern: Strategy pattern as per AR-004
 * Used by: QuotaPolicyEngine and RolePolicyResolver during booking creation
 * 
 * Note: Facility managers can book facilities on behalf of users and have
 * no meaningful quota restrictions for managing their assigned facilities.
 */
@Component
public class FacilityManagerQuotaStrategy extends AbstractQuotaStrategy {

    /**
     * Weekly quota limit for facility manager bookings.
     * Set to 9999 as a practical unlimited value.
     * Facility managers have no meaningful quota restriction.
     */
    private static final int WEEKLY_QUOTA = 9999;

    /**
     * Monthly quota limit for facility manager bookings.
     * Set to 9999 as a practical unlimited value.
     * Facility managers have no meaningful quota restriction.
     */
    private static final int MONTHLY_QUOTA = 9999;

    /**
     * Maximum advance booking window in days.
     * Facility managers can book up to 180 days (6 months) in advance,
     * same as admins, to allow for long-term facility planning.
     */
    private static final int MAX_ADVANCE_BOOKING_DAYS = 180;

    /**
     * Permissiveness priority level.
     * Tied with ADMIN (3), indicating highly permissive policy.
     * Facility managers have similar flexibility to admins for managing their facilities.
     */
    private static final int PERMISSIVENESS_PRIORITY = 3;

    @Override
    public String getRoleName() {
        return "FACILITY_MANAGER";
    }

    @Override
    public boolean canBookDuringPeakHours(LocalTime proposedTime) {
        // Facility managers CAN book during peak hours without restriction
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
    public int getPermissivenessPriority() {
        return PERMISSIVENESS_PRIORITY;
    }

    @Override
    public boolean requiresHighCapacityApproval(int facilityCapacity, int highCapacityThreshold) {
        // Facility managers do NOT require approval for high-capacity facilities
        return false;
    }
}
