package com.sliitreserve.api.strategy.quota;

/**
 * Exception thrown when a booking request violates quota policy constraints.
 * 
 * Common violations:
 * - Exceeds weekly quota limit
 * - Exceeds monthly quota limit
 * - Booking outside allowed advance window
 * - Peak-hour booking restrictions
 * - Unknown or unregistered role
 * 
 * Used by QuotaPolicyEngine and ApprovalWorkflowService to communicate
 * policy violations to callers and API responses (HTTP 400 Bad Request).
 */
public class QuotaPolicyViolationException extends RuntimeException {

    private final String violatedPolicy;
    private final String userRole;

    /**
     * Create a quota policy violation exception.
     * 
     * @param message Human-readable description of the violation
     * @param violatedPolicy Policy name that was violated (e.g., "WEEKLY_QUOTA", "PEAK_HOURS")
     * @param userRole Role of the user attempting the booking
     */
    public QuotaPolicyViolationException(String message, String violatedPolicy, String userRole) {
        super(message);
        this.violatedPolicy = violatedPolicy;
        this.userRole = userRole;
    }

    /**
     * Simple constructor with just a message.
     * Use when policy details are not needed.
     * 
     * @param message Human-readable error message
     */
    public QuotaPolicyViolationException(String message) {
        super(message);
        this.violatedPolicy = "UNKNOWN";
        this.userRole = "UNKNOWN";
    }

    /**
     * Get the specific policy that was violated.
     * 
     * @return Policy name (e.g., "WEEKLY_QUOTA", "PEAK_HOURS", "ADVANCE_WINDOW")
     */
    public String getViolatedPolicy() {
        return violatedPolicy;
    }

    /**
     * Get the role of the user that attempted the booking.
     * 
     * @return Role name
     */
    public String getUserRole() {
        return userRole;
    }
}
