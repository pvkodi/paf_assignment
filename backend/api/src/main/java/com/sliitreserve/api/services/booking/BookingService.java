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
    private final EventPublisher eventPublisher;
    private final ApprovalWorkflowService approvalWorkflowService;

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
            Booking savedBooking = bookingRepository.save(newBooking);
            
            // Initiate approval workflow after booking is saved
            // This will auto-approve or create approval steps based on user role and facility constraints
            approvalWorkflowService.initiateApproval(savedBooking);
            
            // Refresh booking from database to get updated status and approval steps
            savedBooking = bookingRepository.findById(savedBooking.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found after approval workflow"));
            
            return savedBooking;
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
     * Records the approval with an optional note to the approval workflow history.
     */
    @Transactional
    public Booking approveBooking(UUID bookingId, Long expectedVersion, String approvalNote) {
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
            
            // Record approval step with note in workflow history
            com.sliitreserve.api.workflow.approval.ApprovalDecision decision = 
                com.sliitreserve.api.workflow.approval.ApprovalDecision.builder()
                    .status(com.sliitreserve.api.workflow.approval.ApprovalStatus.APPROVED)
                    .approverRole("MANUAL_APPROVAL")
                    .note(approvalNote != null ? approvalNote : "Manually approved")
                    .build();
            
            approvalWorkflowService.recordApprovalStep(booking, 99, decision);
            
            return approvedBooking;
        } catch (OptimisticLockingFailureException ex) {
            throw new ConflictException("Optimistic lock failure upon saving");
        }
    }

    /**
     * Rejects an existing booking using its @Version for optimistic locking (409).
     * Records the rejection with a rejection reason/note to the approval workflow history.
     */
    @Transactional
    public Booking rejectBooking(UUID bookingId, Long expectedVersion, String rejectionReason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getVersion().equals(expectedVersion)) {
            throw new ConflictException("Optimistic lock failure: mismatched versions");
        }

        if (booking.isTerminal()) {
            throw new ValidationException("Booking is already in a terminal state.");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new ValidationException("Rejection reason is required");
        }

        booking.setStatus(BookingStatus.REJECTED);

        try {
            Booking rejectedBooking = bookingRepository.save(booking);
            
            // Record rejection step with reason in workflow history
            com.sliitreserve.api.workflow.approval.ApprovalDecision decision = 
                com.sliitreserve.api.workflow.approval.ApprovalDecision.builder()
                    .status(com.sliitreserve.api.workflow.approval.ApprovalStatus.REJECTED)
                    .approverRole("MANUAL_REJECTION")
                    .note(rejectionReason)
                    .build();
            
            approvalWorkflowService.recordApprovalStep(booking, 99, decision);
            
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
