package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for check-in operations.
 * 
 * Returned on successful check-in (HTTP 200).
 * Includes check-in record details and confirmation message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponseDTO {

    @JsonProperty("check_in_id")
    private UUID checkInId;  // Unique check-in record ID

    @JsonProperty("booking_id")
    private UUID bookingId;  // Associated booking ID

    @JsonProperty("method")
    private String method;  // Check-in method used: "QR" or "MANUAL"

    @JsonProperty("checked_in_at")
    private LocalDateTime checkedInAt;  // Timestamp of check-in (in campus timezone)

    @JsonProperty("message")
    private String message;  // Success message for user (e.g., "✅ Check-in successful!")

    /**
     * Convenience constructor for successful check-in response.
     */
    public static CheckInResponseDTO success(UUID checkInId, UUID bookingId, String method, LocalDateTime checkedInAt) {
        return new CheckInResponseDTO(
            checkInId,
            bookingId,
            method,
            checkedInAt,
            "✅ Check-in successful! Your attendance has been recorded."
        );
    }
}
