package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for Booking entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookingResponseDTO extends BaseResponseDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("facility_id")
    private UUID facilityId;

    @JsonProperty("requested_by_user_id")
    private UUID requestedByUserId;

    @JsonProperty("booked_for_user_id")
    private UUID bookedForUserId;

    @JsonProperty("booking_date")
    private LocalDate bookingDate;

    @JsonProperty("start_time")
    private LocalTime startTime;

    @JsonProperty("end_time")
    private LocalTime endTime;

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("attendees")
    private Integer attendees;

    @JsonProperty("status")
    private String status;

    @JsonProperty("recurrence_rule")
    private String recurrenceRule;

    @JsonProperty("is_recurring_master")
    private Boolean isRecurringMaster;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("version")
    private Long version;
}
