package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API error response DTO.
 * Used for all error responses from the API.
 * Provides consistent error format across all endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO implements BaseDTO {

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("message")
    private String message;

    @JsonProperty("details")
    private String details;

    @JsonProperty("timestamp")
    private Long timestamp;

    public ErrorResponseDTO(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ErrorResponseDTO(String errorCode, String message, String details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }
}
