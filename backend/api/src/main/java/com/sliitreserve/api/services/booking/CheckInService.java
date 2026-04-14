package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.CheckInRecord;
import com.sliitreserve.api.entities.booking.CheckInMethod;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.bookings.CheckInRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * CheckInService for managing booking check-ins and no-show evaluation.
 * 
 * Responsibilities:
 * - Record QR code check-ins (FR-020)
 * - Record manual staff check-ins (FR-020)
 * - Evaluate no-shows within 15-minute grace period (FR-021)
 * - Determine suspension eligibility (FR-022)
 * 
 * Design pattern: Service layer (business logic)
 * Used by: BookingController, dashboards, scheduler jobs
 * 
 * Key Requirements:
 * FR-020: System MUST support check-in via QR code and manual staff check-in.
 * FR-021: System MUST classify no-show when check-in does not occur within 15 minutes 
 *         of booking start in campus local timezone.
 * FR-022: System MUST apply an automatic 1-week suspension after 3 no-shows.
 * 
 * No-Show Classification:
 * A booking is a no-show if:
 * 1. No CheckInRecord exists for the booking, AND
 * 2. Current time (in campus timezone) is more than 15 minutes after booking start time
 * 
 * Grace Period: 15 minutes after booking start time (per FR-021)
 * Example: Booking starts at 14:00, no-show evaluated after 14:15 if no check-in
 * 
 * Suspension Logic (FR-022):
 * When a booking is evaluated as a no-show:
 * 1. User's noShowCount is incremented
 * 2. If noShowCount reaches 3, automatic 1-week suspension is applied
 * 3. Suspension is lifted on appeal approval (T058)
 * 
 * @see CheckInRecord for check-in record entity
 * @see Booking for booking entity
 * @see User for user entity
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Campus local timezone constant.
     * All time-based calculations use this timezone for consistency.
     */
    private static final ZoneId CAMPUS_TIMEZONE = ZoneId.of("Asia/Colombo");

    /**
     * No-show grace period in minutes.
     * Per FR-021: Check-in must occur within 15 minutes of booking start time.
     */
    private static final int NO_SHOW_GRACE_PERIOD_MINUTES = 15;

    /**
     * No-show suspension threshold.
     * Per FR-022: Automatic 1-week suspension applied when threshold is reached (3 no-shows).
     */
    private static final int NO_SHOW_SUSPENSION_THRESHOLD = 3;

    /**
     * Suspension duration in days.
     * Per FR-022: 1-week suspension (7 days).
     */
    private static final int SUSPENSION_DURATION_DAYS = 7;

    /**
     * Record a QR code check-in for a booking.
     * 
     * QR check-ins are typically user-initiated (user scans QR code).
     * The checkedInBy user is optional (may be null for automatic QR scans).
     * 
     * @param bookingId Booking ID for the check-in
     * @param checkedInByUserId Optional user who performed/initiated the check-in (nullable)
     * @return CheckInRecord created
     * @throws ResourceNotFoundException if booking not found
     * @throws ValidationException if booking already has a check-in
     */
    @Transactional
    public CheckInRecord recordQRCheckIn(UUID bookingId, UUID checkedInByUserId) {
        log.info("Recording QR check-in for booking: {}", bookingId);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        
        // Check if booking already has a check-in
        if (checkInRepository.existsByBookingId(bookingId)) {
            log.warn("Booking {} already has a check-in recorded", bookingId);
            throw new ValidationException("Check-in already recorded for this booking");
        }
        
        User checkedInBy = null;
        if (checkedInByUserId != null) {
            checkedInBy = userRepository.findById(checkedInByUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + checkedInByUserId));
        }
        
        LocalDateTime now = LocalDateTime.now(CAMPUS_TIMEZONE);
        
        CheckInRecord checkIn = CheckInRecord.builder()
                .booking(booking)
                .method(CheckInMethod.QR)
                .checkedInBy(checkedInBy)
                .checkedInAt(now)
                .build();
        
        CheckInRecord saved = checkInRepository.save(checkIn);
        log.info("QR check-in recorded: id={}, booking={}, timestamp={}", saved.getId(), bookingId, now);
        
        return saved;
    }

    /**
     * Record a manual staff check-in for a booking.
     * 
     * Manual check-ins are performed by staff/admin (e.g., via attendance sheet, admin interface).
     * The checkedInByUser MUST be provided and should have staff/admin privileges.
     * 
     * @param bookingId Booking ID for the check-in
     * @param checkedInByUserId User ID of the staff member performing check-in (required)
     * @return CheckInRecord created
     * @throws ResourceNotFoundException if booking or user not found
     * @throws ValidationException if booking already has check-in or checkedInByUserId is null
     */
    @Transactional
    public CheckInRecord recordManualCheckIn(UUID bookingId, UUID checkedInByUserId) {
        log.info("Recording manual check-in for booking: {} by staff: {}", bookingId, checkedInByUserId);
        
        if (checkedInByUserId == null) {
            log.warn("Manual check-in requires checkedInByUserId");
            throw new ValidationException("Staff member ID is required for manual check-in");
        }
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        
        User checkedInBy = userRepository.findById(checkedInByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found: " + checkedInByUserId));
        
        // Check if booking already has a check-in
        if (checkInRepository.existsByBookingId(bookingId)) {
            log.warn("Booking {} already has a check-in recorded", bookingId);
            throw new ValidationException("Check-in already recorded for this booking");
        }
        
        LocalDateTime now = LocalDateTime.now(CAMPUS_TIMEZONE);
        
        CheckInRecord checkIn = CheckInRecord.builder()
                .booking(booking)
                .method(CheckInMethod.MANUAL)
                .checkedInBy(checkedInBy)
                .checkedInAt(now)
                .build();
        
        CheckInRecord saved = checkInRepository.save(checkIn);
        log.info("Manual check-in recorded: id={}, booking={}, staff={}, timestamp={}", 
                 saved.getId(), bookingId, checkedInByUserId, now);
        
        return saved;
    }

    /**
     * Evaluate if a booking should be classified as a no-show.
     * 
     * A booking is a no-show if:
     * 1. No CheckInRecord exists for the booking, AND
     * 2. Current time (in campus timezone) is more than 15 minutes after booking start
     * 
     * This method is idempotent - if already evaluated as no-show, returns true again.
     * 
     * Usage:
     * - Called after NO_SHOW_GRACE_PERIOD_MINUTES have passed since booking start
     * - Typically called by a scheduler job that runs periodically (T057 scheduler would call this)
     * - If returns true, may trigger suspension logic (T058 handles suspension)
     * 
     * @param bookingId Booking ID to evaluate
     * @return true if booking is a no-show (no check-in + grace period expired), false otherwise
     * @throws ResourceNotFoundException if booking not found
     */
    @Transactional(readOnly = true)
    public boolean evaluateNoShow(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        
        // Check if check-in record exists
        boolean hasCheckIn = checkInRepository.existsByBookingId(bookingId);
        
        if (hasCheckIn) {
            log.debug("Booking {} has check-in, not a no-show", bookingId);
            return false;
        }
        
        // Check if grace period has expired
        LocalDateTime bookingStart = booking.getBookingDate().atTime(booking.getStartTime());
        LocalDateTime graceDeadline = bookingStart.plusMinutes(NO_SHOW_GRACE_PERIOD_MINUTES);
        LocalDateTime now = LocalDateTime.now(CAMPUS_TIMEZONE);
        
        if (now.isAfter(graceDeadline)) {
            log.info("Booking {} is a no-show (no check-in within {} minutes)", bookingId, NO_SHOW_GRACE_PERIOD_MINUTES);
            return true;
        } else {
            log.debug("Booking {} grace period not yet expired (deadline: {}, now: {})", 
                     bookingId, graceDeadline, now);
            return false;
        }
    }

    /**
     * Get all check-in records for a specific booking.
     * 
     * Typically returns 0 or 1 records (booking may have multiple if data anomaly, but shouldn't).
     * 
     * @param bookingId Booking ID
     * @return List of check-in records for the booking
     */
    @Transactional(readOnly = true)
    public List<CheckInRecord> getCheckInsByBooking(UUID bookingId) {
        log.debug("Fetching check-in records for booking: {}", bookingId);
        List<CheckInRecord> checkIns = checkInRepository.findByBooking_Id(bookingId);
        log.debug("Found {} check-in record(s) for booking: {}", checkIns.size(), bookingId);
        return checkIns;
    }

    /**
     * Check if a booking has been checked in.
     * 
     * @param bookingId Booking ID
     * @return true if at least one check-in record exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isCheckedIn(UUID bookingId) {
        boolean checkedIn = checkInRepository.existsByBookingId(bookingId);
        log.debug("Booking {} checked in: {}", bookingId, checkedIn);
        return checkedIn;
    }

    /**
     * Get the grace period duration in minutes.
     * Public accessor for testing and logging.
     * 
     * @return Grace period duration in minutes (15)
     */
    public static int getNoShowGracePeriodMinutes() {
        return NO_SHOW_GRACE_PERIOD_MINUTES;
    }

    /**
     * Get the no-show suspension threshold.
     * Public accessor for testing and logging.
     * 
     * @return Suspension threshold (3)
     */
    public static int getNoShowSuspensionThreshold() {
        return NO_SHOW_SUSPENSION_THRESHOLD;
    }

    /**
     * Get the suspension duration in days.
     * Public accessor for testing and logging.
     * 
     * @return Suspension duration in days (7)
     */
    public static int getSuspensionDurationDays() {
        return SUSPENSION_DURATION_DAYS;
    }
}
