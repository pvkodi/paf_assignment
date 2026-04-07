package com.sliitreserve.api.controller.advice;

import com.sliitreserve.api.dto.ErrorResponseDTO;
import com.sliitreserve.api.dto.ConflictErrorResponseDTO;
import com.sliitreserve.api.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for all API endpoints.
 * Converts exceptions to standardized error responses aligned with OpenAPI contract.
 * Provides consistent error formatting, logging, and HTTP status mapping.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException.
     * Returns HTTP 404 Not Found with error details.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            ex.getErrorCode(),
            ex.getMessage(),
            new ArrayList<>()
        );
        errorResponse.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handles ValidationException.
     * Returns HTTP 400 Bad Request with validation error details.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            ValidationException ex,
            WebRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getDetails()
        );
        errorResponse.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handles ConflictException.
     * Returns HTTP 409 Conflict with conflict-specific details (version, conflicting resource ID).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ConflictErrorResponseDTO> handleConflictException(
            ConflictException ex,
            WebRequest request) {
        
        log.warn("Conflict occurred: {}", ex.getMessage());
        
        ConflictErrorResponseDTO errorResponse = new ConflictErrorResponseDTO(
            ex.getErrorCode(),
            ex.getMessage(),
            new ArrayList<>(),
            ex.getCurrentVersion(),
            ex.getConflictingResourceId()
        );
        errorResponse.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handles UnauthorizedException.
     * Returns HTTP 401 Unauthorized.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedException(
            UnauthorizedException ex,
            WebRequest request) {
        
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            ex.getErrorCode(),
            ex.getMessage()
        );
        errorResponse.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handles ForbiddenException.
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDTO> handleForbiddenException(
            ForbiddenException ex,
            WebRequest request) {
        
        log.warn("Forbidden access: {}", ex.getMessage());
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            ex.getErrorCode(),
            ex.getMessage()
        );
        errorResponse.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    /**
     * Handles Spring's MethodArgumentNotValidException for bean validation errors.
     * Returns HTTP 400 Bad Request with field validation details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Validation error in request: {}", ex.getMessage());
        
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(String.format("%s: %s", fieldName, errorMessage));
        });
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            "VALIDATION_ERROR",
            "Request validation failed",
            errors
        );
        errorResponse.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Generic exception handler for unexpected errors.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later."
        );
        errorResponse.setTimestamp(LocalDateTime.now());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
