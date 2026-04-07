package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for response DTOs.
 * Extends this class for API response payloads to include common metadata.
 * Includes ID and timestamps for all entities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponseDTO implements BaseDTO {
    
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
