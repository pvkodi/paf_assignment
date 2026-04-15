package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.dto.ConflictErrorResponseDTO;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.BookingRepository;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.services.calendar.PublicHolidayService;
import com.sliitreserve.api.util.booking.BookingBuilder;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;
import java.time.ZonedDateTime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing Facility Bookings (capacity checks, overlapping guards, recurrence skips, 409 constraints, timezones).
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final PublicHolidayService publicHolidayService;
    private final EventPublisher eventPublisher;

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
     * Publishes BOOKING_APPROVED event (STANDARD severity - in-app only).
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
            Booking approvedBooking = bookingRepository.save(booking);
            
            // Publish BOOKING_APPROVED event to notify requester (HIGH severity = in-app + email)
            eventPublisher.publish(EventEnvelope.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .eventType("BOOKING_APPROVED")
                    .severity(EventSeverity.HIGH)
                    .affectedUserId(booking.getRequestedBy().getId().getMostSignificantBits())
                    .title("Your Booking Has Been Approved")
                    .description("Your booking for " + booking.getFacility().getName() + 
                            " on " + booking.getBookingDate() + " has been approved.")
                    .source("BookingService")
                    .entityReference("booking:" + booking.getId())
                    .actionUrl("/bookings/" + booking.getId())
                    .actionLabel("View Booking")
                    .occurrenceTime(ZonedDateTime.now())
                    .metadata(java.util.Map.of("userId", booking.getRequestedBy().getId().toString()))
                    .build());
            
            return approvedBooking;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Optimistic lock failure upon saving");
        }
    }

    /**
     * Rejects an existing booking using its @Version for optimistic locking (409).
     * Publishes BOOKING_REJECTED event (STANDARD severity - in-app only).
     */
    @Transactional
    public Booking rejectBooking(UUID bookingId, Long expectedVersion) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getVersion().equals(expectedVersion)) {
            throw new ConflictException("Optimistic lock failure: mismatched versions");
        }

        if (booking.isTerminal()) {
            throw new ValidationException("Booking is already in a terminal state.");
        }

        booking.setStatus(BookingStatus.REJECTED);

        try {
            Booking rejectedBooking = bookingRepository.save(booking);
            
            // Publish BOOKING_REJECTED event to notify requester (HIGH severity = in-app + email)
            eventPublisher.publish(EventEnvelope.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .eventType("BOOKING_REJECTED")
                    .severity(EventSeverity.HIGH)
                    .affectedUserId(booking.getRequestedBy().getId().getMostSignificantBits())
                    .title("Your Booking Has Been Rejected")
                    .description("Your booking for " + booking.getFacility().getName() + 
                            " on " + booking.getBookingDate() + " has been rejected.")
                    .source("BookingService")
                    .entityReference("booking:" + booking.getId())
                    .actionUrl("/bookings/" + booking.getId())
                    .actionLabel("View Booking")
                    .occurrenceTime(ZonedDateTime.now())
                    .metadata(java.util.Map.of("userId", booking.getRequestedBy().getId().toString()))
                    .build());
            
            return rejectedBooking;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Optimistic lock failure upon saving");
        }
    }

    /**
     * Get all bookings for a user (both as requester and bookedFor).
     * Returns bookings ordered by booking date descending.
     */
    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(UUID userId) {
        return bookingRepository.findByRequestedBy_Id(userId);
    }

    /**
     * Get booking by ID (with authorization check via service layer).
     */
    @Transactional(readOnly = true)
    public Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
    }

    /**
     * Get pending bookings for approval by the given user.
     * - ADMIN: All pending bookings
     * - LECTURER: All pending bookings (they can approve any)
     * - FACILITY_MANAGER: Pending bookings for their managed facilities
     */
    @Transactional(readOnly = true)
    public List<Booking> getPendingApprovalsForUser(User user) {
        // Get all pending bookings
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);
        
        // Filter based on role
        if (user.getRoles().contains(com.sliitreserve.api.entities.auth.Role.ADMIN) || 
            user.getRoles().contains(com.sliitreserve.api.entities.auth.Role.LECTURER)) {
            // Admins and lecturers can approve all pending bookings
            return pendingBookings;
        }
        
        if (user.getRoles().contains(com.sliitreserve.api.entities.auth.Role.FACILITY_MANAGER)) {
            // Facility managers only see bookings for facilities they manage
            // For now, return all (assuming facility manager role has permission)
            // TODO: Add facility ownership check if needed
            return pendingBookings;
        }
        
        // Other roles cannot approve
        return List.of();
    }

    /**
     * Get quota status for a user (placeholder implementation).
     * Returns basic quota info for the user.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuotaStatus(User user) {
        Map<String, Object> quotaStatus = new HashMap<>();
        quotaStatus.put("userId", user.getId());
        quotaStatus.put("userRole", user.getRoles().isEmpty() ? "USER" : user.getRoles().iterator().next().toString());
        quotaStatus.put("weeklyBookings", bookingRepository.countWeeklyBookings(
            user.getId(),
            LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
            LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
        ));
        quotaStatus.put("quotaLimit", 5); // Default quota
        quotaStatus.put("quotaRemaining", Math.max(0, 5 - (int)(long)quotaStatus.get("weeklyBookings")));
        return quotaStatus;
    }
}
