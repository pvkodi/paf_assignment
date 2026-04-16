package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.util.mapping.BookingMapper;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import com.sliitreserve.api.dto.bookings.BookingDetailedResponseDTO;
import com.sliitreserve.api.dto.ConflictErrorResponseDTO;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.calendar.PublicHolidayService;
import com.sliitreserve.api.util.booking.BookingBuilder;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.strategy.quota.QuotaPolicyViolationException;
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
import java.util.stream.Collectors;

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
    private final EventPublisher eventPublisher;
    private final BookingMapper bookingMapper;
    private final ApprovalStepRepository approvalStepRepository;

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
        
        // Check for time bounds inside facility availability
        if (startTime.isBefore(facility.getAvailabilityStart()) || endTime.isAfter(facility.getAvailabilityEnd())) {
            throw new ValidationException("Booking times are outside of the facility's availability window");
        }

        // Capacity validation
        if (attendees == null || attendees < 1 || attendees > facility.getCapacity()) {
            throw new ValidationException("Attendees must be between 1 and facility capacity (" + facility.getCapacity() + ")");
        }

        // Validate against quota policy (may throw QuotaPolicyViolationException)
        try {
            var requestingUser = userRepository.findById(requestedByUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestedByUserId));
            quotaPolicyEngine.validateBookingRequest(requestingUser, bookingDate, startTime, endTime, facility.getCapacity(), 200);
        } catch (QuotaPolicyViolationException qve) {
            throw new ValidationException(qve.getMessage());
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
        
        // Handle recurrence expansion when recurrenceRule provided
        if (recurrenceRule != null && !recurrenceRule.isBlank()) {
            // Save master booking first
            Booking masterBooking;
            try {
                masterBooking = bookingRepository.save(newBooking);
            } catch (OptimisticLockingFailureException e) {
                throw new ConflictException("Data conflict occurred. Version mismatched.");
            }

            // Expand recurrence dates (supports simple FREQ=WEEKLY;BYDAY=...;COUNT=... and FREQ=DAILY;COUNT=...)
            var occurrenceDates = expandRecurrenceDates(bookingDate, recurrenceRule);
            var skipped = new java.util.ArrayList<String>();
            var blockingStatuses = java.util.Arrays.asList(BookingStatus.PENDING, BookingStatus.APPROVED);

            for (var date : occurrenceDates) {
                // skip the master booking date (already represented by masterBooking)
                if (date.equals(bookingDate)) continue;

                if (publicHolidayService.isPublicHoliday(date)) {
                    skipped.add(date.toString());
                    continue;
                }

                boolean overlapping = bookingRepository.existsOverlappingBooking(
                        facilityId, date, startTime, endTime, blockingStatuses
                );
                if (overlapping) {
                    skipped.add(date.toString());
                    continue;
                }

                // quota check per occurrence
                try {
                    var requestingUser = userRepository.findById(requestedByUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestedByUserId));
                    quotaPolicyEngine.validateBookingRequest(requestingUser, date, startTime, endTime, facility.getCapacity(), 200);
                } catch (QuotaPolicyViolationException qve) {
                    skipped.add(date.toString() + ": " + qve.getMessage());
                    continue;
                }

                Booking child = BookingBuilder.builder()
                        .facility(facility)
                        .requestedBy(requestedBy)
                        .bookedFor(bookedFor)
                        .bookingDate(date)
                        .startTime(startTime)
                        .endTime(endTime)
                        .purpose(purpose)
                        .attendees(attendees)
                        .status(BookingStatus.PENDING)
                        .isRecurringMaster(false)
                        .timezone("Asia/Colombo")
                        .build();
                try {
                    bookingRepository.save(child);
                } catch (OptimisticLockingFailureException e) {
                    skipped.add(date.toString() + ": optimistic lock");
                }
            }

            // If any occurrences were skipped, notify requester (include skipped dates in description)
            if (!skipped.isEmpty()) {
                var metadata = new java.util.HashMap<String, Object>();
                metadata.put("skippedDates", skipped);
                metadata.put("recurrenceRule", recurrenceRule);
                metadata.put("facilityId", facilityId.toString());
                String joined = String.join(", ", skipped);
                String desc = "Some occurrences in your recurring booking were skipped due to holidays, conflicts or policy violations.";
                if (!joined.isBlank()) {
                    desc = desc + " Skipped: " + joined;
                }

                eventPublisher.publish(EventEnvelope.builder()
                        .eventType("BOOKING_RECURRING_SKIPPED")
                        .severity(EventSeverity.STANDARD)
                        .title("Recurring Booking - Some occurrences skipped")
                        .description(desc)
                        .source("BookingService")
                        .entityReference("booking:" + masterBooking.getId())
                        .actionUrl("/bookings/" + masterBooking.getId())
                        .actionLabel("View Booking")
                        .metadata(metadata)
                        .build());
            }

            return masterBooking;
        }

        // Check For overlapping (non-recurring)
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
     * Expand a very small subset of RRULE syntax to concrete dates.
     * Supports:
     * - FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=N
     * - FREQ=DAILY;COUNT=N
     *
     * For unsupported rules, returns a singleton list with the startDate.
     */
    private java.util.List<java.time.LocalDate> expandRecurrenceDates(java.time.LocalDate startDate, String rrule) {
        var dates = new java.util.ArrayList<java.time.LocalDate>();
        if (rrule == null || rrule.isBlank()) {
            dates.add(startDate);
            return dates;
        }

        try {
            var parts = java.util.Arrays.stream(rrule.split(";"))
                    .map(s -> s.split("=", 2))
                    .filter(arr -> arr.length == 2)
                    .collect(java.util.stream.Collectors.toMap(a -> a[0].toUpperCase(), a -> a[1].toUpperCase()));

            var freq = parts.getOrDefault("FREQ", "");
            int count = parts.containsKey("COUNT") ? Integer.parseInt(parts.get("COUNT")) : 12;

            if ("WEEKLY".equals(freq) && parts.containsKey("BYDAY")) {
                var bydayStr = parts.get("BYDAY");
                var tokens = java.util.Arrays.stream(bydayStr.split(",")).map(String::trim).toArray(String[]::new);
                var dayOfWeeks = new java.util.ArrayList<java.time.DayOfWeek>();
                for (String t : tokens) {
                    switch (t) {
                        case "MO": dayOfWeeks.add(java.time.DayOfWeek.MONDAY); break;
                        case "TU": dayOfWeeks.add(java.time.DayOfWeek.TUESDAY); break;
                        case "WE": dayOfWeeks.add(java.time.DayOfWeek.WEDNESDAY); break;
                        case "TH": dayOfWeeks.add(java.time.DayOfWeek.THURSDAY); break;
                        case "FR": dayOfWeeks.add(java.time.DayOfWeek.FRIDAY); break;
                        case "SA": dayOfWeeks.add(java.time.DayOfWeek.SATURDAY); break;
                        case "SU": dayOfWeeks.add(java.time.DayOfWeek.SUNDAY); break;
                    }
                }

                java.time.LocalDate cursor = startDate;
                while (dates.size() < count) {
                    if (!cursor.isBefore(startDate) && dayOfWeeks.contains(cursor.getDayOfWeek())) {
                        dates.add(cursor);
                    }
                    cursor = cursor.plusDays(1);
                    // Safety break to avoid infinite loop
                    if (cursor.isAfter(startDate.plusYears(2))) break;
                }
                return dates;
            } else if ("DAILY".equals(freq)) {
                java.time.LocalDate cursor = startDate;
                while (dates.size() < count) {
                    dates.add(cursor);
                    cursor = cursor.plusDays(1);
                }
                return dates;
            }
        } catch (Exception e) {
            // Fall through and return singleton
            e.printStackTrace();
        }

        dates.add(startDate);
        return dates;
    }

    /**
     * Approves an existing booking using its @Version for optimistic locking (409).
     */
    @Transactional
    public Booking approveBooking(UUID bookingId, Long expectedVersion, String note) {
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
    public Booking rejectBooking(UUID bookingId, Long expectedVersion, String note) {
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
    /**
     * Get all bookings for a user (detailed DTOs with nested objects).
     * Used for the my-bookings page to display full booking information.
     */
    @Transactional(readOnly = true)
    public List<BookingDetailedResponseDTO> getUserBookingsDetailed(UUID userId) {
        List<Booking> bookings = bookingRepository.findUserBookingsWithDetails(userId);
        return bookings.stream()
                .map(bookingMapper::toDetailedResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all bookings for a user (basic DTOs with IDs only).
     * Returns bookings where user is either the requester or the bookedFor user.
     */
    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(UUID userId) {
        return bookingRepository.findByRequestedBy_Id(userId);
    }

    /**
     * Get a specific booking by ID (detailed DTO with nested objects).
     * Used for displaying full booking information.
     */
    @Transactional(readOnly = true)
    public BookingDetailedResponseDTO getBookingDetailed(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        return bookingMapper.toDetailedResponseDTO(booking);
    }

    /**
     * Get booking by ID (with authorization check via service layer).
     */
    @Transactional(readOnly = true)
    public Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
    }

    /**     * Get pending bookings that require approval with detailed information.
     * Returns bookings with full nested objects for display purposes.
     */
    @Transactional(readOnly = true)
    public List<BookingDetailedResponseDTO> getPendingApprovalsDetailed(User user) {
        List<Booking> pendingBookings = getPendingApprovalsForUser(user);
        return pendingBookings.stream()
                .map(bookingMapper::toDetailedResponseDTO)
                .collect(Collectors.toList());
    }

    /**     * Get pending bookings for approval by the given user.
     * - ADMIN: All pending bookings
     * - LECTURER: All pending bookings (they can approve any)
     * - FACILITY_MANAGER: Pending bookings for their managed facilities
     */
    @Transactional(readOnly = true)
    public List<Booking> getPendingApprovalsForUser(User user) {
        // Get all pending bookings with eager loaded relationships
        List<Booking> pendingBookings = bookingRepository.findByStatusWithDetails(BookingStatus.PENDING);
        
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
     * Get comprehensive quota status for a user.
     * Uses QuotaPolicyEngine to determine the user's effective role and quota limits.
     * Returns both current usage and limits for weekly and monthly quotas.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getQuotaStatus(User user) {
        Map<String, Object> quotaStatus = new HashMap<>();
        
        // Get effective role strategy
        com.sliitreserve.api.strategy.quota.QuotaStrategy strategy = quotaPolicyEngine.resolveEffectiveStrategy(user);
        String effectiveRole = strategy.getRoleName();
        
        // Get current date ranges
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Colombo"));
        LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        
        // Count current bookings (PENDING and APPROVED only)
        long weeklyBookings = bookingRepository.countWeeklyBookings(user.getId(), weekStart, weekEnd);
        long monthlyBookings = bookingRepository.countMonthlyBookings(user.getId(), monthStart, monthEnd);
        
        // Get quota limits
        int weeklyQuota = strategy.getWeeklyQuota();
        int monthlyQuota = strategy.getMonthlyQuota();
        
        // Advance booking info
        int maxAdvanceDays = strategy.getMaxAdvanceBookingDays();
        LocalDate advanceBookingUntil = today.plusDays(maxAdvanceDays);
        
        // Peak hours info (08:00-10:00)
        boolean canBookDuringPeakHours = strategy.canBookDuringPeakHours(LocalTime.of(9, 0));
        
        // Build response
        quotaStatus.put("userId", user.getId());
        quotaStatus.put("effectiveRole", effectiveRole);
        
        quotaStatus.put("weeklyBookings", weeklyBookings);
        quotaStatus.put("weeklyQuota", weeklyQuota);
        quotaStatus.put("weeklyRemaining", Math.max(0, weeklyQuota - (int)weeklyBookings));
        
        quotaStatus.put("monthlyBookings", monthlyBookings);
        quotaStatus.put("monthlyQuota", monthlyQuota);
        quotaStatus.put("monthlyRemaining", Math.max(0, monthlyQuota - (int)monthlyBookings));
        
        quotaStatus.put("canBookDuringPeakHours", canBookDuringPeakHours);
        quotaStatus.put("peakHourWindow", "08:00-10:00");
        
        quotaStatus.put("advanceBookingDays", maxAdvanceDays);
        quotaStatus.put("advanceBookingUntil", advanceBookingUntil.toString());
        
        return quotaStatus;
    }

    /**
     * Get available timeslots for a facility on a specific date.
     * Returns a list of booked time ranges to help user avoid conflicts.
     * 
     * @param facilityId Facility ID
     * @param bookingDate Date to check
     * @return List of booked timeslots as DTOs
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableTimeslots(UUID facilityId, LocalDate bookingDate) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));

        // Get all confirmed bookings for this date
        List<Booking> confirmedBookings = bookingRepository.findConfirmedBookingsByFacilityAndDate(facilityId, bookingDate);
        
        List<Map<String, Object>> bookedSlots = new java.util.ArrayList<>();
        
        for (Booking booking : confirmedBookings) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("startTime", booking.getStartTime().toString());
            slot.put("endTime", booking.getEndTime().toString());
            slot.put("status", booking.getStatus().toString());
            slot.put("purpose", booking.getPurpose());
            slot.put("attendees", booking.getAttendees());
            slot.put("bookedBy", booking.getBookedFor().getDisplayName());
            bookedSlots.add(slot);
        }

        // Also include facility availability window
        Map<String, Object> facilityInfo = new HashMap<>();
        facilityInfo.put("facilityId", facilityId);
        facilityInfo.put("facilityName", facility.getName());
        facilityInfo.put("availabilityStart", facility.getAvailabilityStart().toString());
        facilityInfo.put("availabilityEnd", facility.getAvailabilityEnd().toString());
        facilityInfo.put("bookedSlots", bookedSlots);
        
        // Calculate free slots
        List<Map<String, String>> freeSlots = calculateFreeSlots(
            facility.getAvailabilityStart(),
            facility.getAvailabilityEnd(),
            confirmedBookings
        );
        facilityInfo.put("freeSlots", freeSlots);
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        result.add(facilityInfo);
        return result;
    }

    /**
     * Calculate free/available time slots between booked time ranges.
     */
    private List<Map<String, String>> calculateFreeSlots(LocalTime facilityStart, LocalTime facilityEnd, 
                                                          List<Booking> bookings) {
        List<Map<String, String>> freeSlots = new java.util.ArrayList<>();
        
        if (bookings.isEmpty()) {
            // Entire day is free
            Map<String, String> slot = new HashMap<>();
            slot.put("startTime", facilityStart.toString());
            slot.put("endTime", facilityEnd.toString());
            freeSlots.add(slot);
            return freeSlots;
        }

        LocalTime currentTime = facilityStart;
        for (Booking booking : bookings) {
            if (currentTime.isBefore(booking.getStartTime())) {
                Map<String, String> slot = new HashMap<>();
                slot.put("startTime", currentTime.toString());
                slot.put("endTime", booking.getStartTime().toString());
                freeSlots.add(slot);
            }
            currentTime = booking.getEndTime();
            if (currentTime.isAfter(facilityEnd)) currentTime = facilityEnd;
        }

        // Add final free slot if space remains
        if (currentTime.isBefore(facilityEnd)) {
            Map<String, String> slot = new HashMap<>();
            slot.put("startTime", currentTime.toString());
            slot.put("endTime", facilityEnd.toString());
            freeSlots.add(slot);
        }

        return freeSlots;
    }

    /**
     * Get all bookings for admin/facility-manager dashboard.
     * Filters by status and date range. Facility managers see only their facilities.
     * 
     * @param facilityId Optional facility ID filter
     * @param statuses Booking statuses to include
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of bookings sorted by date
     */
    @Transactional(readOnly = true)
    public List<Booking> getAdminBookings(UUID facilityId, List<BookingStatus> statuses, 
                                          LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findBookingsByDateRangeAndFacility(
            facilityId,
            statuses != null && !statuses.isEmpty() ? statuses : Arrays.asList(BookingStatus.APPROVED, BookingStatus.PENDING),
            startDate,
            endDate
        );
    }
}
