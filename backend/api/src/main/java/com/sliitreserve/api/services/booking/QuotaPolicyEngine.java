package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.strategy.quota.QuotaStrategy;
import com.sliitreserve.api.strategy.quota.QuotaPolicyViolationException;
import com.sliitreserve.api.strategy.quota.RolePolicyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

/**
 * QuotaPolicyEngine for validating booking requests against quota constraints.
 * 
 * Responsibilities:
 * - Use effective-role resolver (RolePolicyResolver) to determine most permissive applicable strategy for user
 * - Validate booking requests against quota limits (weekly, monthly)
 * - Validate peak-hour restrictions
 * - Validate advance booking window limits
 * - Determine if high-capacity facility approval is required
 * 
 * Design pattern: Service layer (business logic) + Strategy pattern (quota policies)
 * Used by: BookingService and ApprovalWorkflowService during booking creation/approval
 * 
 * AR-004 requirement: Quota evaluation behavior uses Strategy pattern (implemented via QuotaStrategy interface)
 * FR-042 requirement: For multi-role users, most permissive eligible role policy is applied
 * 
 * Quota Enforcement Rules (FR-018, FR-019):
 * - USER: 3 per week, 10 per month, peak-hour restrictions apply, 90-day advance window
 * - LECTURER: 99 per week (unlimited), 999 per month (unlimited), peak-hours allowed, 90-day advance window
 * - ADMIN: 9999 per week (unlimited), 9999 per month (unlimited), peak-hours allowed, 180-day advance window
 * 
 * Counting Rules:
 * - Only PENDING and APPROVED bookings are counted against quotas
 * - REJECTED and CANCELLED bookings are not counted
 * - Week is calendar week (Monday-Sunday) in campus local timezone
 * - Month is calendar month in campus local timezone
 * 
 * @see QuotaStrategy for role-specific policies
 * @see RolePolicyResolver for multi-role policy resolution
 * @see BookingRepository for quota counting queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaPolicyEngine {

    private final RolePolicyResolver rolePolicyResolver;
    private final BookingRepository bookingRepository;

    /**
     * Campus local timezone constant (consistent with AbstractQuotaStrategy).
     * All time-based calculations use this timezone.
     */
    private static final ZoneId CAMPUS_TIMEZONE = ZoneId.of("Asia/Colombo");

    /**
     * Validate a booking request against all applicable quota constraints.
     * 
     * This is the main entry point for quota validation. It performs all checks and throws
     * QuotaPolicyViolationException if any constraint is violated.
     * 
     * Validation sequence:
     * 1. Resolve effective strategy for user (most permissive if multi-role)
     * 2. Check advance booking window
     * 3. Check peak-hour restrictions
     * 4. Check weekly quota limit
     * 5. Check monthly quota limit
     * 6. Check high-capacity approval requirement
     * 
     * @param user User performing the booking
     * @param proposedDate Booking date in campus local timezone
     * @param proposedStartTime Booking start time
     * @param proposedEndTime Booking end time
     * @param facilityCapacity Capacity of the facility being booked
     * @param highCapacityThreshold Configured high-capacity threshold (typically per facility type)
     * @throws QuotaPolicyViolationException if any constraint is violated
     */
    public void validateBookingRequest(
            User user,
            LocalDate proposedDate,
            LocalTime proposedStartTime,
            LocalTime proposedEndTime,
            int facilityCapacity,
            int highCapacityThreshold) {
        
        log.debug("Validating booking request for user: {} on date: {}", user.getId(), proposedDate);
        
        // Resolve effective policy for user
        QuotaStrategy effectiveStrategy = resolveEffectiveStrategy(user);
        log.debug("Effective quota strategy for user: {}", effectiveStrategy.getRoleName());
        
        // Check advance booking window
        validateAdvanceBookingWindow(proposedDate, effectiveStrategy);
        
        // Check peak-hour restrictions
        validatePeakHourRestrictions(proposedStartTime, effectiveStrategy);
        
        // Check weekly quota
        validateWeeklyQuota(user, proposedDate, effectiveStrategy);
        
        // Check monthly quota
        validateMonthlyQuota(user, proposedDate, effectiveStrategy);
        
        // Log high-capacity approval requirement (info only, not a hard rejection)
        if (effectiveStrategy.requiresHighCapacityApproval(facilityCapacity, highCapacityThreshold)) {
            log.info("Booking requires high-capacity facility approval for role: {} and facility capacity: {}",
                     effectiveStrategy.getRoleName(), facilityCapacity);
        }
        
        log.debug("All quota validations passed for user: {} and date: {}", user.getId(), proposedDate);
    }

    /**
     * Determine if high-capacity facility approval is required for this booking.
     * Used by ApprovalWorkflowService to determine if FACILITY_MANAGER sign-off is needed.
     * 
     * @param user User performing the booking
     * @param facilityCapacity Capacity of the facility being booked
     * @param highCapacityThreshold Configured high-capacity threshold
     * @return true if high-capacity approval is required, false otherwise
     */
    public boolean requiresHighCapacityApproval(User user, int facilityCapacity, int highCapacityThreshold) {
        QuotaStrategy effectiveStrategy = resolveEffectiveStrategy(user);
        boolean required = effectiveStrategy.requiresHighCapacityApproval(facilityCapacity, highCapacityThreshold);
        
        if (required) {
            log.debug("High-capacity approval required: strategy={}, capacity={}, threshold={}",
                     effectiveStrategy.getRoleName(), facilityCapacity, highCapacityThreshold);
        }
        
        return required;
    }

    /**
     * Resolve the effective (most permissive) quota strategy for a user.
     * 
     * This handles multi-role users by selecting the strategy with the highest permissiveness priority.
     * Single-role users simply return their associated strategy.
     * 
     * @param user User to resolve strategy for
     * @return Effects strategy (most permissive among user's roles)
     * @throws IllegalArgumentException if user has no roles or roles are not recognized
     */
    private QuotaStrategy resolveEffectiveStrategy(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            log.warn("User {} has no roles assigned", user.getId());
            throw new IllegalArgumentException("User must have at least one role");
        }
        
        // Convert Role enum set to String set for RolePolicyResolver
        var roleNames = user.getRoles().stream()
                .map(Role::name)
                .toList();
        
        try {
            QuotaStrategy strategy = rolePolicyResolver.resolveStrategy(roleNames);
            log.debug("Resolved strategy {} for user {} with roles: {}", 
                     strategy.getRoleName(), user.getId(), roleNames);
            return strategy;
        } catch (IllegalArgumentException e) {
            log.error("Failed to resolve strategy for user {} with roles: {}", user.getId(), roleNames, e);
            throw e;
        }
    }

    /**
     * Validate that the proposed booking date is within the advance booking window.
     * 
     * Each strategy has a maximum advance booking window (e.g., 90 days for USER, 180 for ADMIN).
     * Bookings must be for today or within the future window specified by the strategy.
     * 
     * @param proposedDate Booking date to validate
     * @param strategy Effective quota strategy for the user
     * @throws QuotaPolicyViolationException if booking is outside advance window
     */
    private void validateAdvanceBookingWindow(LocalDate proposedDate, QuotaStrategy strategy) {
        if (!strategy.canBookWithinAdvanceWindow(proposedDate)) {
            String message = String.format(
                    "Booking date %s exceeds the maximum advance booking window of %d days for role %s",
                    proposedDate, strategy.getMaxAdvanceBookingDays(), strategy.getRoleName());
            log.warn("Advance booking window violation: {}", message);
            throw new QuotaPolicyViolationException(message, "ADVANCE_BOOKING_WINDOW", strategy.getRoleName());
        }
        log.debug("Advance booking window validation passed for date: {}", proposedDate);
    }

    /**
     * Validate that the proposed booking start time does not violate peak-hour restrictions.
     * 
     * Peak-hour restrictions are enforced only for USER role (08:00-10:00 in campus timezone).
     * Other roles (LECTURER, ADMIN) can book during any time.
     * 
     * @param proposedStartTime Start time of the booking
     * @param strategy Effective quota strategy for the user
     * @throws QuotaPolicyViolationException if booking violates peak-hour restrictions
     */
    private void validatePeakHourRestrictions(LocalTime proposedStartTime, QuotaStrategy strategy) {
        if (!strategy.canBookDuringPeakHours(proposedStartTime)) {
            String message = String.format(
                    "Booking start time %s falls within peak hours (08:00-10:00) which are not allowed for role %s",
                    proposedStartTime, strategy.getRoleName());
            log.warn("Peak-hour restriction violation: {}", message);
            throw new QuotaPolicyViolationException(message, "PEAK_HOURS", strategy.getRoleName());
        }
        log.debug("Peak-hour restriction validation passed for time: {}", proposedStartTime);
    }

    /**
     * Validate that the user has not exceeded their weekly booking quota.
     * 
     * Week is defined as Monday-Sunday (ISO 8601) in campus local timezone.
     * Counts both PENDING and APPROVED bookings.
     * 
     * @param user User to validate
     * @param proposedDate Booking date (used to determine which week to count)
     * @param strategy Effective quota strategy for the user
     * @throws QuotaPolicyViolationException if weekly quota is exceeded
     */
    private void validateWeeklyQuota(User user, LocalDate proposedDate, QuotaStrategy strategy) {
        // Calculate the Monday of the given week (ISO 8601, Monday = 1, Sunday = 7)
        LocalDate weekStart = proposedDate.minusDays(proposedDate.get(ChronoField.DAY_OF_WEEK) - 1);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        long currentWeeklyBookings = bookingRepository.countWeeklyBookings(user.getId(), weekStart, weekEnd);
        int weeklyQuota = strategy.getWeeklyQuota();
        
        log.debug("Weekly quota check: current={}, limit={}, user={}, week={} to {}",
                 currentWeeklyBookings, weeklyQuota, user.getId(), weekStart, weekEnd);
        
        if (currentWeeklyBookings >= weeklyQuota) {
            String message = String.format(
                    "User has reached weekly booking quota of %d for role %s (current: %d)",
                    weeklyQuota, strategy.getRoleName(), currentWeeklyBookings);
            log.warn("Weekly quota violation: {}", message);
            throw new QuotaPolicyViolationException(message, "WEEKLY_QUOTA", strategy.getRoleName());
        }
        
        log.debug("Weekly quota validation passed for user: {}", user.getId());
    }

    /**
     * Validate that the user has not exceeded their monthly booking quota.
     * 
     * Month is defined as calendar month (1-31) in campus local timezone.
     * Counts both PENDING and APPROVED bookings.
     * 
     * @param user User to validate
     * @param proposedDate Booking date (used to determine which month to count)
     * @param strategy Effective quota strategy for the user
     * @throws QuotaPolicyViolationException if monthly quota is exceeded
     */
    private void validateMonthlyQuota(User user, LocalDate proposedDate, QuotaStrategy strategy) {
        // Calculate the first and last day of the given month
        LocalDate monthStart = proposedDate.withDayOfMonth(1);
        LocalDate monthEnd = proposedDate.withDayOfMonth(proposedDate.lengthOfMonth());
        
        long currentMonthlyBookings = bookingRepository.countMonthlyBookings(user.getId(), monthStart, monthEnd);
        int monthlyQuota = strategy.getMonthlyQuota();
        
        log.debug("Monthly quota check: current={}, limit={}, user={}, month={} to {}",
                 currentMonthlyBookings, monthlyQuota, user.getId(), monthStart, monthEnd);
        
        if (currentMonthlyBookings >= monthlyQuota) {
            String message = String.format(
                    "User has reached monthly booking quota of %d for role %s (current: %d)",
                    monthlyQuota, strategy.getRoleName(), currentMonthlyBookings);
            log.warn("Monthly quota violation: {}", message);
            throw new QuotaPolicyViolationException(message, "MONTHLY_QUOTA", strategy.getRoleName());
        }
        
        log.debug("Monthly quota validation passed for user: {}", user.getId());
    }
}
