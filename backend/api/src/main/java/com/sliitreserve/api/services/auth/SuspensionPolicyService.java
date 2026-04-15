package com.sliitreserve.api.services.auth;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.repositories.auth.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Suspension Policy Service for enforcing suspended user access restrictions.
 *
 * <p><b>Purpose</b>: Manages user suspension lifecycle and enforces FR-003 (suspended users
 * blocked from protected operations except auth/session, profile view, and appeal submission).
 *
 * <p><b>Suspension Workflow</b> (related to FR-022, FR-023):
 * <ul>
 *   <li>User reaches 3 no-shows → automatic 1-week suspension applied (suspendedUntil = now + 7 days)
 *   <li>While suspended: allowed operations = auth, profile view, appeal submission
 *   <li>While suspended: blocked operations = bookings, approvals, check-ins, ticketing
 *   <li>Admin reviews appeal and accepts → suspension lifted (suspendedUntil = null, noShowCount reset)
 *   <li>Admin reviews appeal and rejects → suspension continues
 * </ul>
 *
 * <p><b>API Contract</b> (FR-003):
 * <ul>
 *   <li>Suspended users attempting blocked operations receive HTTP 403 Forbidden
 *   <li>Error response includes suspension end date and appeal submission guidance
 *   <li>Suspended users CAN access: /auth/profile, /appeals (POST), /auth/logout
 * </ul>
 *
 * <p><b>Integration Points</b>:
 * <ul>
 *   <li>BookingService: Calls checkSuspensionPolicy() before creating booking
 *   <li>CheckInService: Calls applySuspensionIfThresholdReached() after no-show recorded
 *   <li>AppealService: Calls releaseSuspension() when appeal approved
 *   <li>Controllers: Calls checkSuspensionPolicy() for all protected endpoints except whitelist
 * </ul>
 *
 * @see User for suspension fields (suspendedUntil, noShowCount)
 * @see com.sliitreserve.api.services.booking.CheckInService for no-show recording
 * @see com.sliitreserve.api.services.auth.AppealService for appeal processing
 */
@Service
@Slf4j
public class SuspensionPolicyService {

    // Suspension constants
    private static final int NO_SHOW_SUSPENSION_THRESHOLD = 3;
    private static final int SUSPENSION_DAYS = 7;
    private static final String SUSPENSION_REASON_TEMPLATE = 
        "Account suspended until %s due to 3 no-shows. Submit an appeal to request reinstatement.";

    @Autowired
    private UserRepository userRepository;

    /**
     * Check if user is currently suspended and throw exception if attempting protected operation.
     *
     * <p>This is the primary enforcement point for FR-003. Call this before any protected
     * operation except whitelisted actions (auth, profile, appeals).
     *
     * <p><b>Whitelisted Operations</b> (always allowed for suspended users):
     * <ul>
     *   <li>GET /auth/profile - view own profile
     *   <li>POST /auth/logout - end session
     *   <li>POST /appeals - submit suspension appeal
     * </ul>
     *
     * <p><b>Blocked Operations</b> (throw exception if user is suspended):
     * <ul>
     *   <li>POST /bookings - create booking request
     *   <li>POST /bookings/{id}/approve - approve booking
     *   <li>POST /bookings/{id}/check-in - check in to booking
     *   <li>POST /tickets - create ticket
     *   <li>POST /tickets/{id}/comments - comment on ticket
     *   <li>All other protected endpoints
     * </ul>
     *
     * @param user User entity to check suspension status
     * @throws ForbiddenException if user is currently suspended with suspension end date and guidance
     */
    public void checkSuspensionPolicy(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (isSuspended(user)) {
            String suspensionMessage = String.format(
                "Your account is suspended until %s. You can submit an appeal through the appeals section.",
                user.getSuspendedUntil()
            );
            log.warn("Suspended user {} attempted protected operation", user.getEmail());
            throw new ForbiddenException(suspensionMessage);
        }
    }

    /**
     * Check if user is currently suspended.
     *
     * <p>A user is considered suspended if suspendedUntil is set and in the future.
     *
     * @param user User entity to check
     * @return true if user is currently suspended; false if not suspended or suspension has expired
     */
    public boolean isSuspended(User user) {
        if (user == null || user.getSuspendedUntil() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        boolean suspended = user.getSuspendedUntil().isAfter(now);

        if (suspended) {
            log.debug("User {} is suspended until {}", user.getEmail(), user.getSuspendedUntil());
        }

        return suspended;
    }

    /**
     * Record a no-show and automatically apply suspension if threshold is reached.
     *
     * <p>Implements FR-022: automatic 1-week suspension after 3 no-shows.
     * Called by CheckInService when a booking check-in fails (no check-in within 15 min of start).
     *
     * @param user User entity to update with no-show
     * @return User entity with updated noShowCount and possibly new suspension
     * @throws IllegalArgumentException if user is null
     */
    @Transactional
    public User recordNoShowAndApplySuspensionIfNeeded(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // Increment no-show counter
        int currentNoShowCount = user.getNoShowCount() != null ? user.getNoShowCount() : 0;
        currentNoShowCount++;
        user.setNoShowCount(currentNoShowCount);

        log.debug("Recorded no-show for user {}. Total no-shows: {}", user.getEmail(), currentNoShowCount);

        // Check if threshold reached
        if (currentNoShowCount >= NO_SHOW_SUSPENSION_THRESHOLD) {
            applySuspension(user);
        }

        // Save updated user
        return userRepository.save(user);
    }

    /**
     * Manually apply suspension to a user for 7 days from now.
     *
     * <p><b>Note</b>: Typically called internally by recordNoShowAndApplySuspensionIfNeeded().
     * May also be called manually by admin for policy violations.
     *
     * @param user User entity to suspend
     */
    private void applySuspension(User user) {
        LocalDateTime suspendedUntil = LocalDateTime.now(ZoneId.systemDefault())
                .plusDays(SUSPENSION_DAYS);
        
        user.setSuspendedUntil(suspendedUntil);
        log.warn("Suspended user {} until {} due to reaching no-show threshold", 
            user.getEmail(), suspendedUntil);
    }

    /**
     * Release suspension and reset no-show counter (called when appeal is approved).
     *
     * <p>Implements FR-023: admin user can approve appeal → suspension lifted + noShowCount reset.
     * Called by AppealService when an appeal is approved.
     *
     * @param user User entity to release from suspension
     * @return User entity with suspension cleared and no-show count reset
     * @throws IllegalArgumentException if user is null
     */
    @Transactional
    public User releaseSuspension(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        user.setSuspendedUntil(null);
        user.setNoShowCount(0);

        log.info("Released suspension for user {} and reset no-show counter", user.getEmail());

        return userRepository.save(user);
    }

    /**
     * Get human-readable suspension reason message for a suspended user.
     *
     * <p>Used in error responses and UI messages to explain suspension to user.
     *
     * @param user User entity (assumed to be suspended)
     * @return Human-readable suspension message with end date and appeal guidance
     */
    public String getSuspensionMessage(User user) {
        if (user == null || user.getSuspendedUntil() == null) {
            return "User is not suspended";
        }

        return String.format(SUSPENSION_REASON_TEMPLATE, user.getSuspendedUntil());
    }

    /**
     * Check if user can perform the given operation type while suspended.
     *
     * <p><b>Whitelisted Operations</b> (return true even if suspended):
     * <ul>
     *   <li>"profile" - view own profile
     *   <li>"logout" - end session
     *   <li>"appeal" - submit suspension appeal
     * </ul>
     *
     * <p><b>All Other Operations</b> (return false if user is suspended).
     *
     * @param user User entity to check
     * @param operationType Operation type string (case-insensitive)
     * @return true if operation is allowed for suspended user or user is not suspended
     */
    public boolean isOperationAllowedForSuspendedUser(User user, String operationType) {
        if (user == null || !isSuspended(user)) {
            return true; // Operation allowed if user not suspended
        }

        // Whitelist check
        String normalizedType = operationType != null ? operationType.toLowerCase() : "";
        boolean isWhitelisted = normalizedType.equals("profile") || 
                               normalizedType.equals("logout") || 
                               normalizedType.equals("appeal");

        if (isWhitelisted) {
            log.debug("Whitelisted operation '{}' allowed for suspended user {}", 
                operationType, user.getEmail());
        }

        return isWhitelisted;
    }

    /**
     * Get no-show count for a user.
     *
     * @param user User entity
     * @return Current no-show count (0 if null)
     */
    public int getNoShowCount(User user) {
        if (user == null || user.getNoShowCount() == null) {
            return 0;
        }
        return user.getNoShowCount();
    }

    /**
     * Get remaining no-shows until suspension.
     *
     * @param user User entity
     * @return Remaining no-shows allowed before automatic suspension (0 if already at threshold)
     */
    public int getRemainingNoShowsBeforeSuspension(User user) {
        int currentCount = getNoShowCount(user);
        int remaining = NO_SHOW_SUSPENSION_THRESHOLD - currentCount;
        return Math.max(0, remaining);
    }

    /**
     * Check if user has reached no-show threshold and needs suspension.
     *
     * @param user User entity
     * @return true if user has 3 or more no-shows
     */
    public boolean hasReachedNoShowThreshold(User user) {
        return getNoShowCount(user) >= NO_SHOW_SUSPENSION_THRESHOLD;
    }

    /**
     * Get suspension threshold constant.
     *
     * @return Number of no-shows required for automatic suspension
     */
    public int getSuspensionThreshold() {
        return NO_SHOW_SUSPENSION_THRESHOLD;
    }

    /**
     * Get suspension duration in days.
     *
     * @return Number of days for each suspension period
     */
    public int getSuspensionDurationDays() {
        return SUSPENSION_DAYS;
    }

    /**
     * Apply suspension if user has reached the no-show threshold (3 no-shows).
     *
     * <p>Called by NoShowScheduler after a no-show has been recorded and no-show count incremented.
     * Checks if the count has reached 3, and if so, applies a 7-day suspension.
     *
     * <p>Used in the context where the user's no-show count has already been incremented.
     *
     * @param user User entity that may be eligible for suspension
     * @return User with suspension applied if threshold reached, unchanged otherwise
     */
    @Transactional
    public User applySuspensionIfThresholdReached(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (hasReachedNoShowThreshold(user)) {
            applySuspension(user);
            return userRepository.save(user);
        }

        return user;
    }
}
