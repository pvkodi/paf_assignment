package com.sliitreserve.api.exception;

import org.springframework.http.HttpStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when request validation fails.
 * Maps to HTTP 400 Bad Request response.
 * Supports multiple validation error details.
 */
public class ValidationException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    private final List<String> details;

    /**
     * Constructor with a single validation error message.
     */
    public ValidationException(String message) {
        super(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            message
        );
        this.details = new ArrayList<>();
        this.details.add(message);
    }

    /**
     * Constructor with multiple validation error details.
     */
    public ValidationException(String message, List<String> details) {
        super(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            message
        );
        this.details = details != null ? new ArrayList<>(details) : new ArrayList<>();
    }

    public List<String> getDetails() {
        return details;
    }
}
