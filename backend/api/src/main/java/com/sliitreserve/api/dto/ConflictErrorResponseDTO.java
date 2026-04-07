package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API error response DTO for conflict scenarios (HTTP 409).
 * Extends ErrorResponseDTO with conflict-specific fields.
 * Used for optimistic locking conflicts and booking overlaps.
 * Aligns with OpenAPI contract: ConflictErrorResponse schema.
 */
@Getter
@Setter
@NoArgsConstructor
public class ConflictErrorResponseDTO extends ErrorResponseDTO {

    private static final long serialVersionUID = 1L;

    @JsonProperty("current_version")
    private Integer currentVersion;

    @JsonProperty("conflicting_booking_id")
    private UUID conflictingBookingId;

    /**
     * Constructor with error details and conflict-specific fields.
     */
    public ConflictErrorResponseDTO(String code, String message, List<String> details,
                                     Integer currentVersion, UUID conflictingBookingId) {
        super(code, message, details);
        this.currentVersion = currentVersion;
        this.conflictingBookingId = conflictingBookingId;
        this.setTimestamp(LocalDateTime.now());
    }
}
