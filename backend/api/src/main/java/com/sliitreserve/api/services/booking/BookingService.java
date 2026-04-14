package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.dto.ConflictErrorResponseDTO;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.exception.ConflictException;
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
import java.util.List;
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
}
