package com.sliitreserve.api.services.auth;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduler for cleaning up expired user suspensions from the database.
 * 
 * Responsibilities:
 * - Run periodically (every 30 minutes)
 * - Find users whose suspension time has passed
 * - Clear suspendedUntil field to null in database
 * - Maintains database consistency
 * 
 * Purpose:
 * - Automatic cleanup when suspension time expires
 * - Ensures suspendedUntil field doesn't contain stale dates
 * - Validates that isSuspended() logic is consistent across the system
 * 
 * Note:
 * - The isSuspended() method in SuspensionPolicyService also clears suspensions on-demand
 * - This scheduler provides periodic cleanup for comprehensive database maintenance
 * - Both mechanisms work together: on-demand (isSuspended) + scheduled (this job)
 * 
 * @see SuspensionPolicyService#isSuspended(User) for on-demand auto-unsuspend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuspensionCleanupScheduler {

    private final UserRepository userRepository;

    /**
     * Campus local timezone constant.
     */
    private static final ZoneId CAMPUS_TIMEZONE = ZoneId.of("Asia/Colombo");

    /**
     * Scheduled job to clean up expired suspensions.
     * 
     * Runs every 30 minutes (1800000 milliseconds) after 10-minute startup delay.
     * 
     * Process:
     * 1. Get current time in campus timezone
     * 2. Find all users with suspendedUntil in the past
     * 3. Clear suspendedUntil field for each
     * 4. Log cleanup results
     * 
     * This ensures:
     * - Database doesn't accumulate stale suspension dates
     * - Backup consistency if deployment changes
     * - Audit trail shows when suspensions actually expired
     */
    @Scheduled(initialDelay = 600000, fixedDelay = 1800000) // Run every 30 minutes after 10-minute startup delay
    @Transactional
    public void cleanupExpiredSuspensions() {
        log.debug("Starting suspension cleanup job");
        
        try {
            LocalDateTime now = LocalDateTime.now(CAMPUS_TIMEZONE);
            
            // Find users with expired suspensions
            List<User> expiredUsers = userRepository.findBySuspendedUntilNotNullAndSuspendedUntilBefore(now);
            
            if (expiredUsers.isEmpty()) {
                log.debug("No expired suspensions to clean up");
                return;
            }
            
            log.info("Cleaning up {} expired suspension(s)", expiredUsers.size());
            
            int cleanedCount = 0;
            
            for (User user : expiredUsers) {
                try {
                    log.debug("Clearing suspension for user: {} (was suspended until: {})", 
                             user.getEmail(), user.getSuspendedUntil());
                    
                    user.setSuspendedUntil(null);
                    userRepository.save(user);
                    cleanedCount++;
                    
                } catch (Exception e) {
                    log.error("Error cleaning up suspension for user {}", user.getId(), e);
                    // Continue with next user despite error
                }
            }
            
            log.info("Suspension cleanup completed. Cleaned: {} records", cleanedCount);
            
        } catch (Exception e) {
            log.error("Fatal error in suspension cleanup scheduler", e);
            // Log error but don't throw - allow job to continue next cycle
        }
    }
}
