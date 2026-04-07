package com.sliitreserve.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Maps to HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            String.format("%s with identifier '%s' not found", resourceName, identifier)
        );
    }

    public ResourceNotFoundException(String message) {
        super(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            message
        );
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            message,
            cause
        );
    }
}
