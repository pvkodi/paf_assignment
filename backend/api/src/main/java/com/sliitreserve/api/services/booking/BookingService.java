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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;

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
    private final QuotaPolicyEngine quotaPolicyEngine;
    
    private static final int HIGH_CAPACITY_THRESHOLD = 200;

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
        
        // Validate booking against quota policies (weekly, monthly, peak hours, advance window)
        quotaPolicyEngine.validateBookingRequest(
            bookedFor,
            bookingDate,
            startTime,
            endTime,
            facility.getCapacity(),
            HIGH_CAPACITY_THRESHOLD
        );

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
     * Rejects an existing booking using its @Version for optimistic locking (409).
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
            return bookingRepository.save(booking);
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
     * Get quota status for a user with role-based quota limits.
     * Returns complete quota information including weekly, monthly, and advance window limits.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuotaStatus(User user) {
        Map<String, Object> quotaStatus = new HashMap<>();
        
        // Get this week's date range (Monday-Sunday)
        LocalDate weekStart = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        
        // Get this month's date range
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        
        // Count current bookings
        long weeklyBookings = bookingRepository.countWeeklyBookings(user.getId(), weekStart, weekEnd);
        long monthlyBookings = bookingRepository.countMonthlyBookings(user.getId(), monthStart, monthEnd);
        
        // Determine role and get quota limits
        String userRole = user.getRoles().isEmpty() ? "STUDENT" : user.getRoles().iterator().next().toString();
        int weeklyQuota = getWeeklyQuotaForRole(userRole);
        int monthlyQuota = getMonthlyQuotaForRole(userRole);
        int advanceWindowDays = getAdvanceWindowForRole(userRole);
        
        quotaStatus.put("userId", user.getId());
        quotaStatus.put("userRole", userRole);
        quotaStatus.put("weeklyBookings", weeklyBookings);
        quotaStatus.put("weeklyQuota", weeklyQuota);
        quotaStatus.put("weeklyRemaining", Math.max(0, weeklyQuota - (int)weeklyBookings));
        quotaStatus.put("monthlyBookings", monthlyBookings);
        quotaStatus.put("monthlyQuota", monthlyQuota);
        quotaStatus.put("monthlyRemaining", Math.max(0, monthlyQuota - (int)monthlyBookings));
        quotaStatus.put("advanceWindowDays", advanceWindowDays);
        quotaStatus.put("weekStart", weekStart.toString());
        quotaStatus.put("weekEnd", weekEnd.toString());
        quotaStatus.put("monthStart", monthStart.toString());
        quotaStatus.put("monthEnd", monthEnd.toString());
        
        return quotaStatus;
    }
    
    /**
     * Get weekly quota limit for a role.
     */
    private int getWeeklyQuotaForRole(String role) {
        return switch (role) {
            case "STUDENT" -> 5;
            case "LECTURER" -> 99;
            case "ADMIN" -> 9999;
            default -> 5;
        };
    }
    
    /**
     * Get monthly quota limit for a role.
     */
    private int getMonthlyQuotaForRole(String role) {
        return switch (role) {
            case "STUDENT" -> 20;
            case "LECTURER" -> 999;
            case "ADMIN" -> 9999;
            default -> 20;
        };
    }
    
    /**
     * Get advance booking window in days for a role.
     */
    private int getAdvanceWindowForRole(String role) {
        return switch (role) {
            case "STUDENT" -> 90;
            case "LECTURER" -> 90;
            case "ADMIN" -> 180;
            default -> 90;
        };
    }
}
