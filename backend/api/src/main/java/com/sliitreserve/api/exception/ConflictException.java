package com.sliitreserve.api.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

/**
 * Exception thrown when a request conflicts with existing data.
 * Maps to HTTP 409 Conflict response.
 * Used for optimistic locking conflicts, duplicate resources, booking overlaps, etc.
 */
public class ConflictException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    private final Integer currentVersion;
    private final UUID conflictingResourceId;

    /**
     * Constructor for booking overlap/concurrency conflict with version and resource.
     */
    public ConflictException(String message, Integer currentVersion, UUID conflictingResourceId) {
        super(
            HttpStatus.CONFLICT,
            "CONFLICT",
            message
        );
        this.currentVersion = currentVersion;
        this.conflictingResourceId = conflictingResourceId;
    }

    /**
     * Constructor for general conflict without version/resource details.
     */
    public ConflictException(String message) {
        super(
            HttpStatus.CONFLICT,
            "CONFLICT",
            message
        );
        this.currentVersion = null;
        this.conflictingResourceId = null;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public UUID getConflictingResourceId() {
        return conflictingResourceId;
    }
}
