package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for creating or updating a Booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookingRequestDTO extends BaseRequestDTO {

    @NotNull(message = "Facility ID is required")
    @JsonProperty("facility_id")
    private UUID facilityId;

    @JsonProperty("booked_for_user_id")
    private UUID bookedForUserId; // Used for admin on-behalf bookings

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date must be today or in the future")
    @JsonProperty("booking_date")
    private LocalDate bookingDate;

    @NotNull(message = "Start time is required")
    @JsonProperty("start_time")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonProperty("end_time")
    private LocalTime endTime;

    @NotBlank(message = "Purpose is required")
    @JsonProperty("purpose")
    private String purpose;

    @NotNull(message = "Attendees count is required")
    @Min(value = 1, message = "At least 1 attendee is required")
    @JsonProperty("attendees")
    private Integer attendees;

    @JsonProperty("recurrence_rule")
    private String recurrenceRule;
}
