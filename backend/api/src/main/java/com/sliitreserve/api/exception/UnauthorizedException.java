package com.sliitreserve.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication is required but not provided or invalid.
 * Maps to HTTP 401 Unauthorized response.
 */
public class UnauthorizedException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            message
        );
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            message,
            cause
        );
    }
}
