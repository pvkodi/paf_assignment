package com.sliitreserve.api.strategy.quota;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Strategy interface for quota and booking policy enforcement.
 * 
 * Encapsulates role-specific policies for:
 * - Booking quota limits (per week, per month)
 * - Peak-hour restrictions
 * - Maximum advance booking window
 * - High-capacity facility approval requirements
 * 
 * Implementations (Student, Lecturer, Admin) define constraints specific to each role.
 * RolePolicyResolver selects the most permissive applicable strategy when users have multiple roles.
 * 
 * Usage:
 * <pre>
 * QuotaStrategy strategy = resolver.resolveStrategy(user.getRoles());
 * if (!strategy.canBookDuringPeakHours(proposedTime)) {
 *     throw new QuotaViolationException("Peak hour restriction applies");
 * }
 * if (!strategy.canBookWithinAdvanceWindow(proposedDate)) {
 *     throw new QuotaViolationException("Advance booking window exceeded");
 * }
 * </pre>
 */
public interface QuotaStrategy {

    /**
     * Get the human-readable name of this quota strategy role.
     * Used for logging, error messages, and debugging.
     * 
     * @return Role name (e.g., "USER", "LECTURER", "ADMIN")
     */
    String getRoleName();

    /**
     * Check if user can book during peak hours for this role.
     * Peak hours defined as 08:00-10:00 in campus local timezone.
     * 
     * @param proposedTime Time of booking request
     * @return true if booking is allowed during peak hours, false if restricted
     */
    boolean canBookDuringPeakHours(LocalTime proposedTime);

    /**
     * Get maximum bookings allowed per week for this role.
     * "Per week" is Monday-Sunday in campus local timezone.
     * 
     * @return Quota limit per week (e.g., 3 for student, 10 for lecturer)
     */
    int getWeeklyQuota();

    /**
     * Get maximum bookings allowed per month for this role.
     * "Per month" is calendar month in campus local timezone.
     * 
     * @return Quota limit per month (e.g., 10 for student, 30 for lecturer)
     */
    int getMonthlyQuota();

    /**
     * Get maximum advance booking window for this role in days.
     * Bookings beyond this window from today are rejected.
     * 
     * @return Maximum days in advance allowed (e.g., 90 for student, 180 for admin)
     */
    int getMaxAdvanceBookingDays();

    /**
     * Check if proposed booking date respects the advance booking window.
     * Compares proposedDate against today + maxAdvanceBookingDays in campus timezone.
     * 
     * @param proposedDate Booking date requested
     * @return true if within advance window, false if beyond limit
     */
    boolean canBookWithinAdvanceWindow(LocalDate proposedDate);

    /**
     * Check if high-capacity facility approval is required for this role.
     * High-capacity threshold is facility-dependent (typically configured per location).
     * 
     * @param facilityCapacity Capacity of facility being booked
     * @param highCapacityThreshold Configured high-capacity threshold for comparison
     * @return true if additional facility-manager sign-off is required, false if not
     */
    boolean requiresHighCapacityApproval(int facilityCapacity, int highCapacityThreshold);

    /**
     * Get the priority order for multi-role policy resolution.
     * Higher values indicate more permissive policies.
     * Used by RolePolicyResolver to select most permissive role.
     * 
     * Example:
     * - Student: priority 1 (most restrictive)
     * - Lecturer: priority 2 (moderate)
     * - Admin: priority 3 (most permissive)
     * 
     * @return Priority order for policy selection (higher = more permissive)
     */
    int getPermissivenessPriority();
}
