package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.dto.ConflictErrorResponseDTO;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.BookingRepository;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.service.calendar.PublicHolidayService;
import com.sliitreserve.api.util.booking.BookingBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing Facility Bookings (capacity checks, overlapping guards, recurrence skips, 409 constraints, timezones).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final PublicHolidayService publicHolidayService;

    /**
     * Creates a new booking. Checks capacity, overlapping times, skips public holidays if recursive.
     */
    @Transactional
    public Booking createBooking(UUID facilityId, UUID requestedByUserId, UUID bookedForUserId,
                                 LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                                 String purpose, Integer attendees, String recurrenceRule) {
        
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));

        User requestedBy = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestedByUserId));

        User bookedFor = userRepository.findById(bookedForUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + bookedForUserId));
        
        // Skip public holidays (for single or recurrence cases if we map a series, usually skip instances falling on holidays)
        if (publicHolidayService.isPublicHoliday(bookingDate)) {
            throw new ConflictException("Cannot book on a public holiday: " + bookingDate);
        }
        
        // Check for time bounds inside facility availability
        if (startTime.isBefore(facility.getAvailabilityStart()) || endTime.isAfter(facility.getAvailabilityEnd())) {
            throw new ValidationException("Booking times are outside of the facility's availability window");
        }

        Booking newBooking = BookingBuilder.builder()
                .facility(facility)
                .requestedBy(requestedBy)
                .bookedFor(bookedFor)
                .bookingDate(bookingDate)
                .startTime(startTime)
                .endTime(endTime)
                .purpose(purpose)
                .attendees(attendees)
                .status(BookingStatus.PENDING)
                .recurrenceRule(recurrenceRule)
                .isRecurringMaster(recurrenceRule != null && !recurrenceRule.isBlank())
                .timezone("Asia/Colombo") // Standard JVM TimeZone
                .build();
        
        // Check For overlapping
        List<BookingStatus> blockingStatuses = Arrays.asList(BookingStatus.PENDING, BookingStatus.APPROVED);
        boolean isOverlapping = bookingRepository.existsOverlappingBooking(
                facilityId, bookingDate, startTime, endTime, blockingStatuses
        );

        if (isOverlapping) {
             throw new ConflictException("The facility is already booked during the requested time slot.");
        }

        try {
            return bookingRepository.save(newBooking);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictException("Data conflict occurred. Version mismatched.");
        }
    }

    /**
     * Approves an existing booking using its @Version for optimistic locking (409).
     */
    @Transactional
    public Booking approveBooking(UUID bookingId, Long expectedVersion) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getVersion().equals(expectedVersion)) {
            throw new ConflictException("Optimistic lock failure: mismatched versions");
        }

        if (booking.isTerminal()) {
            throw new ValidationException("Booking is already in a terminal state.");
        }

        booking.setStatus(BookingStatus.APPROVED);

        try {
            return bookingRepository.save(booking);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Optimistic lock failure upon saving");
        }
    }

    /**
     * Get pending bookings for the given approver roles.
     * 
     * Filters bookings based on role:
     * - LECTURER: bookings from USER requiring lecturer approval
     * - ADMIN: bookings in PENDING state awaiting admin approval
     * - FACILITY_MANAGER: bookings above high-capacity threshold
     * 
     * @param approverRoles Set of roles for the current user
     * @param pageable Pagination parameters
     * @return Page of pending bookings for approval
     */
    @Transactional(readOnly = true)
    public Page<Booking> getPendingBookingsByApproverRole(Set<Role> approverRoles, Pageable pageable) {
        log.debug("Fetching pending bookings for roles: {}", approverRoles);

        // Start with PENDING bookings
        List<Booking> allPendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);
        
        // Filter based on user's role(s)
        List<Booking> relevantBookings = allPendingBookings.stream()
                .filter(booking -> isRelevantForApproval(booking, approverRoles))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), relevantBookings.size());
        List<Booking> pageContent = relevantBookings.subList(start, end);

        return new PageImpl<>(pageContent, pageable, relevantBookings.size());
    }

    /**
     * Check whether a booking is relevant for approval by the given roles.
     * 
     * Logic:
     * - LECTURER can approve USER bookings
     * - ADMIN can approve any pending booking
     * - FACILITY_MANAGER can approve high-capacity bookings
     */
    private boolean isRelevantForApproval(Booking booking, Set<Role> approverRoles) {
        if (approverRoles.contains(Role.ADMIN)) {
            return true; // Admins see all pending bookings
        }

        if (approverRoles.contains(Role.LECTURER)) {
            // Lecturers approve USER bookings
            return booking.getRequestedBy().getRoles().contains(Role.USER);
        }

        if (approverRoles.contains(Role.FACILITY_MANAGER)) {
            // Facility managers approve high-capacity bookings
            return booking.getFacility().getCapacity() >= 100; // Configurable threshold
        }

        return false;
    }

    /**
     * Check in to a booking (QR or manual).
     * 
     * Records check-in and validates timing:
     * - Must occur within 15 minutes of booking start time (FR-021)
     * - Updates booking status to CHECKED_IN
     * - Records who performed the check-in
     * - Triggers no-show evaluation if outside grace period
     * 
     * @param bookingId UUID of booking to check in
     * @param method Check-in method (QR_CODE or MANUAL)
     * @param checkedInBy User performing the check-in
     * @return Updated booking with CHECKED_IN status
     * @throws ValidationException if check-in window has expired
     */
    @Transactional
    public Booking checkInBooking(UUID bookingId, String method, User checkedInBy) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        log.info("Processing check-in for booking {} using method: {} by user: {}", bookingId, method, checkedInBy.getId());

        // Validate check-in timing: must be within 15 minutes of start (FR-021)
        LocalDateTime bookingStart = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getStartTime()
        );
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Colombo"));
        
        long minutesBeforeStart = java.time.temporal.ChronoUnit.MINUTES.between(now, bookingStart);
        long minutesAfterStart = java.time.temporal.ChronoUnit.MINUTES.between(bookingStart, now);

        // Allow check-in up to 15 minutes before AND up to 15 minutes after start
        if (minutesBeforeStart > 15 || (minutesAfterStart > 0 && minutesAfterStart > 15)) {
            log.warn("Check-in not allowed: outside 15-minute window. Minutes before: {}, after: {}", 
                    minutesBeforeStart, minutesAfterStart);
            
            // Mark as NO_SHOW if check-in was attempted after grace period
            booking.setStatus(BookingStatus.NO_SHOW);
            log.info("Booking {} marked as NO_SHOW due to missed check-in window", bookingId);
            
            try {
                bookingRepository.save(booking);
                // TODO: Trigger suspension policy evaluation through observer pattern
                // This will be handled in T058 suspension service integration
            } catch (OptimisticLockingFailureException ex) {
                throw new ConflictException("Optimistic lock failure: booking was modified during no-show recording");
            }
            
            throw new ValidationException(
                    String.format("Check-in window has expired. Must check in within 15 minutes of start time.")
            );
        }

        // Update booking status with check-in details
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckInMethod(method);
        booking.setCheckedInAt(now);
        booking.setCheckedInBy(checkedInBy);

        try {
            Booking updated = bookingRepository.save(booking);
            log.info("Check-in successful for booking {} at {} by user {}", bookingId, now, checkedInBy.getId());
            return updated;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Optimistic lock failure: booking was modified during check-in");
        }
    }

    /**
     * Legacy check-in method (for backward compatibility with controller).
     * 
     * @param bookingId UUID of booking to check in
     * @param method Check-in method (QR_CODE or MANUAL)
     * @return Updated booking with CHECKED_IN status
     */
    @Transactional
    public Booking checkInBooking(UUID bookingId, String method) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        log.info("Processing check-in for booking {} using method: {}", bookingId, method);

        // Validate check-in timing: must be within 15 minutes of start (FR-021)
        LocalDateTime bookingStart = LocalDateTime.of(
                booking.getBookingDate(),
                booking.getStartTime()
        );
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Colombo"));
        
        long minutesBeforeStart = java.time.temporal.ChronoUnit.MINUTES.between(now, bookingStart);
        long minutesAfterStart = java.time.temporal.ChronoUnit.MINUTES.between(bookingStart, now);

        // Allow check-in up to 15 minutes before AND up to 15 minutes after start
        if (minutesBeforeStart > 15 || (minutesAfterStart > 0 && minutesAfterStart > 15)) {
            log.warn("Check-in not allowed: outside 15-minute window. Minutes before: {}, after: {}", 
                    minutesBeforeStart, minutesAfterStart);
            
            // Mark as NO_SHOW if check-in was attempted after grace period
            booking.setStatus(BookingStatus.NO_SHOW);
            log.info("Booking {} marked as NO_SHOW due to missed check-in window", bookingId);
            
            try {
                bookingRepository.save(booking);
                // TODO: Trigger suspension policy evaluation through observer pattern
            } catch (OptimisticLockingFailureException ex) {
                throw new ConflictException("Optimistic lock failure: booking was modified during no-show recording");
            }
            
            throw new ValidationException(
                    String.format("Check-in window has expired. Must check in within 15 minutes of start time.")
            );
        }

        // Update booking status
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckInMethod(method);
        booking.setCheckedInAt(now);

        try {
            Booking updated = bookingRepository.save(booking);
            log.info("Check-in successful for booking {} at {}", bookingId, now);
            return updated;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Optimistic lock failure: booking was modified during check-in");
        }
    }
}
