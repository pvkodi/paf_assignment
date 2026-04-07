package com.sliitreserve.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is authenticated but lacks required authorization.
 * Maps to HTTP 403 Forbidden response.
 */
public class ForbiddenException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            message
        );
    }

    public ForbiddenException(String message, Throwable cause) {
        super(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            message,
            cause
        );
    }
}
