package com.sliitreserve.api.dto.bookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.booking.CheckInMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for check-in operations.
 * 
 * Supports both QR and manual check-in methods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequestDTO {

    @NotNull(message = "Check-in method is required (QR or MANUAL)")
    @JsonProperty("method")
    private CheckInMethod method;

    @JsonProperty("notes")
    private String notes;
}
