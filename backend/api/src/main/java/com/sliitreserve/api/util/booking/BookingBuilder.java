package com.sliitreserve.api.util.booking;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Utility builder to construct a {@link Booking} entity smoothly with validation or calculated defaults
 * where necessary.
 *
 * <p><b>Purpose</b>: Assures all required fields of a Booking are set appropriately before persistence,
 * particularly dealing with recurring instances, statuses, and capacity limits if desired.
 */
public class BookingBuilder {

    private Facility facility;
    private User requestedBy;
    private User bookedFor;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private Integer attendees;
    private BookingStatus status = BookingStatus.PENDING;
    private String recurrenceRule;
    private boolean isRecurringMaster = false;
    private String timezone = "Asia/Colombo";

    private BookingBuilder() {
    }

    public static BookingBuilder builder() {
        return new BookingBuilder();
    }

    public BookingBuilder facility(Facility facility) {
        this.facility = facility;
        return this;
    }

    public BookingBuilder requestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
        return this;
    }

    public BookingBuilder bookedFor(User bookedFor) {
        this.bookedFor = bookedFor;
        return this;
    }

    public BookingBuilder bookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
        return this;
    }

    public BookingBuilder startTime(LocalTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public BookingBuilder endTime(LocalTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public BookingBuilder purpose(String purpose) {
        this.purpose = purpose;
        return this;
    }

    public BookingBuilder attendees(Integer attendees) {
        this.attendees = attendees;
        return this;
    }

    public BookingBuilder status(BookingStatus status) {
        this.status = status;
        return this;
    }

    public BookingBuilder recurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
        return this;
    }

    public BookingBuilder isRecurringMaster(boolean isRecurringMaster) {
        this.isRecurringMaster = isRecurringMaster;
        return this;
    }

    public BookingBuilder timezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public Booking build() {
        Booking booking = new Booking();
        booking.setFacility(facility);
        booking.setRequestedBy(requestedBy);
        booking.setBookedFor(bookedFor);
        booking.setBookingDate(bookingDate);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPurpose(purpose);
        booking.setAttendees(attendees);
        booking.setStatus(status);
        booking.setRecurrenceRule(recurrenceRule);
        booking.setRecurringMaster(isRecurringMaster);
        booking.setTimezone(timezone);
        booking.setVersion(0L); // Default version for optimistic locking
        
        // Optional validations here if needed, such as isValidTimeRange() and isValidCapacity()
        if (!booking.isValidTimeRange()) {
            throw new IllegalArgumentException("Booking start time must be before end time");
        }
        
        if (!booking.isValidCapacity()) {
            throw new IllegalArgumentException("Booking attendees cannot be null and must not exceed facility capacity");
        }

        return booking;
    }
}
