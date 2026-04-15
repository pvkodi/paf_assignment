package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for CheckInRecord entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CheckInResponseDTO extends com.sliitreserve.api.dto.BaseResponseDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("booking_id")
    private UUID bookingId;

    @JsonProperty("method")
    private String method;

    @JsonProperty("checked_in_by_user_id")
    private UUID checkedInByUserId;

    @JsonProperty("checked_in_at")
    private LocalDateTime checkedInAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
