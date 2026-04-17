package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced Booking Response DTO with complete nested information.
 * Includes facility details, user information, and approval workflow.
 * Used for detailed booking views in the UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookingDetailedResponseDTO extends com.sliitreserve.api.dto.BaseResponseDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("facility")
    private FacilitySummaryDTO facility;

    @JsonProperty("requested_by")
    private UserSummaryDTO requestedBy;

    @JsonProperty("booked_for")
    private UserSummaryDTO bookedFor;

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

    @JsonProperty("approvalSteps")
    private List<ApprovalStepDTO> approvalSteps;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
