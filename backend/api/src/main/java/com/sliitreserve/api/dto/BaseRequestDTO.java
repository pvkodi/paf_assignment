package com.sliitreserve.api.dto;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for request DTOs.
 * Extends this class for API request payloads to ensure consistency.
 */
@Getter
@Setter
public class BaseRequestDTO implements BaseDTO {
    
    private static final long serialVersionUID = 1L;
}
