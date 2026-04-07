package com.sliitreserve.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all custom API exceptions.
 * All domain-specific exceptions should extend this to provide consistent error handling.
 */
public abstract class BaseApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus httpStatus;
    private final String errorCode;

    /**
     * Constructor with HTTP status and error code.
     *
     * @param httpStatus HTTP status code for this error
     * @param errorCode  Application-level error code (e.g., "RESOURCE_NOT_FOUND")
     * @param message    Human-readable error message
     */
    public BaseApiException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    /**
     * Constructor with HTTP status, error code, message, and cause.
     */
    public BaseApiException(HttpStatus httpStatus, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
