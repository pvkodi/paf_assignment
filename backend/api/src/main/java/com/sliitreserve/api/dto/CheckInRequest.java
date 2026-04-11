package com.sliitreserve.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Check-in Request DTO
 * 
 * Supports both QR code scanning and manual staff check-in.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    /**
     * Check-in method: QR_CODE or MANUAL
     * - QR_CODE: Student scans QR at facility entrance
     * - MANUAL: Staff manually records check-in
     */
    @NotBlank(message = "Check-in method is required (QR_CODE or MANUAL)")
    private String method;

    /**
     * QR code or token (optional, used for QR_CODE method)
     */
    private String qrToken;

    /**
     * Notes or reason for manual check-in (optional)
     */
    private String notes;
}
