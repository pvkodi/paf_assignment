package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.auth.SuspensionPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * NoShowScheduler for automatically evaluating and processing no-shows.
 * 
 * Responsibilities:
 * - Run periodically (every 10 minutes)
 * - Find bookings that are past their grace period without check-ins
 * - Evaluate each booking for no-show status
 * - Apply suspension when threshold (3 no-shows) is reached
 * 
 * Requirements:
 * - FR-021: System MUST classify no-show when check-in does not occur within 15 minutes 
 *           of booking start in campus local timezone.
 * - FR-022: System MUST apply an automatic 1-week suspension after 3 no-shows.
 * 
 * Grace Period: 15 minutes after booking start time
 * Evaluation occurs: After booking start time + 15 minutes grace period
 * 
 * Scheduling:
 * - Runs every 10 minutes (600000 ms)
 * - Uses 5-minute initial delay to allow system startup
 * - Runs asynchronously without blocking other operations
 * - Errors are caught and logged; job continues processing remaining bookings
 * 
 * Integration Points:
 * - BookingRepository: Find bookings needing evaluation
 * - CheckInService: Evaluate booking for no-show
 * - UserRepository: Get user to check/apply suspension
 * - SuspensionPolicyService: Apply suspension when threshold reached
 * 
 * @see CheckInService for no-show evaluation logic
 * @see SuspensionPolicyService for suspension application
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoShowScheduler {

    private final BookingRepository bookingRepository;
    private final CheckInService checkInService;
    private final UserRepository userRepository;
    private final SuspensionPolicyService suspensionPolicyService;

    /**
     * Campus local timezone constant.
     * All time-based calculations use this timezone for consistency.
     */
    private static final ZoneId CAMPUS_TIMEZONE = ZoneId.of("Asia/Colombo");

    /**
     * Scheduled job to evaluate no-shows and apply suspensions.
     * 
     * Runs every 10 minutes (600000 milliseconds).
     * Initial delay of 5 minutes (300000 milliseconds) to allow system startup.
     * 
     * Process:
     * 1. Calculate cutoff time (current time - grace period buffer)
     * 2. Find all bookings past this cutoff without check-ins
     * 3. For each booking:
     *    a. Evaluate if truly a no-show (service handles time calculations)
     *    b. If no-show: increment user's noShowCount
     *    c. If noShowCount >= 3: apply 1-week suspension
     * 4. Log all results and handle errors gracefully
     */
    @Scheduled(initialDelay = 300000, fixedDelay = 600000) // Run every 10 minutes after 5-minute startup delay
    @Transactional
    public void evaluateNoShows() {
        log.info("Starting scheduled no-show evaluation job");
        
        try {
            LocalDateTime now = LocalDateTime.now(CAMPUS_TIMEZONE);
            LocalTime cutoffTime = now.toLocalTime();
            
            log.debug("Cutoff time for no-show evaluation: {}", cutoffTime);
            
            // Find bookings that should be evaluated for no-shows
            List<Booking> bookingsToEvaluate = bookingRepository.findBookingsForNoShowEvaluation(cutoffTime);
            
            if (bookingsToEvaluate.isEmpty()) {
                log.debug("No bookings found for no-show evaluation");
                return;
            }
            
            log.info("Found {} bookings to evaluate for no-shows", bookingsToEvaluate.size());
            
            int processedCount = 0;
            int noShowCount = 0;
            int suspensionCount = 0;
            
            // Evaluate each booking
            for (Booking booking : bookingsToEvaluate) {
                try {
                    if (checkInService.evaluateNoShow(booking.getId())) {
                        noShowCount++;
                        log.info("Booking {} evaluated as no-show", booking.getId());
                        
                        // Increment no-show count and check for suspension
                        User bookedForUser = booking.getBookedFor();
                        if (bookedForUser != null) {
                            // Check if user is ADMIN or FACILITY_MANAGER (exempt from suspension)
                            boolean isPrivilegedUser = bookedForUser.getRoles() != null && 
                                (bookedForUser.getRoles().contains(Role.ADMIN) || 
                                 bookedForUser.getRoles().contains(Role.FACILITY_MANAGER));
                            
                            if (isPrivilegedUser) {
                                log.info("No-show recorded for privileged user {} (exempted from suspension)", bookedForUser.getId());
                                // Still increment for auditing, but don't suspend
                                int currentNoShowCount = bookedForUser.getNoShowCount() != null ? bookedForUser.getNoShowCount() : 0;
                                bookedForUser.setNoShowCount(currentNoShowCount + 1);
                                userRepository.save(bookedForUser);
                            } else {
                                int currentNoShowCount = bookedForUser.getNoShowCount() != null ? bookedForUser.getNoShowCount() : 0;
                                int newNoShowCount = currentNoShowCount + 1;
                                
                                bookedForUser.setNoShowCount(newNoShowCount);
                                log.debug("User {} no-show count incremented to {}", bookedForUser.getId(), newNoShowCount);
                                
                                // Check if suspension threshold is reached (3 no-shows)
                                if (newNoShowCount >= 3) {
                                    log.info("User {} reached no-show suspension threshold (noShowCount={})", 
                                             bookedForUser.getId(), newNoShowCount);
                                    
                                    suspensionPolicyService.applySuspensionIfThresholdReached(bookedForUser);
                                    suspensionCount++;
                                    
                                    log.info("Suspension applied to user {} due to 3 no-shows", bookedForUser.getId());
                                }
                                
                                userRepository.save(bookedForUser);
                            }
                        }
                    }
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    log.error("Error evaluating booking {} for no-show", booking.getId(), e);
                    // Continue processing other bookings despite this error
                }
            }
            
            log.info("No-show evaluation job completed. " +
                     "Processed: {}, No-shows: {}, Suspensions applied: {}", 
                     processedCount, noShowCount, suspensionCount);
            
        } catch (Exception e) {
            log.error("Fatal error in no-show evaluation scheduler", e);
            // Log error but don't throw - allow job to continue next cycle
        }
    }
}
