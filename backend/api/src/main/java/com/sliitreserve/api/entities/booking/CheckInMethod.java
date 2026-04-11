package com.sliitreserve.api.entities.booking;

/**
 * Check-in method enumeration.
 * 
 * Represents the method used to record a check-in for a booking.
 * Per FR-020: System supports both QR code and manual staff check-in methods.
 */
public enum CheckInMethod {
    /**
     * QR code check-in.
     * User scans a QR code (e.g., on a booking credential, facility entrance, or mobile app).
     * Typically automated or user-initiated.
     */
    QR("QR Code"),
    
    /**
     * Manual staff check-in.
     * Staff member records check-in manually (e.g., via attendance sheet, admin interface, or staff app).
     * Requires staff/admin role to perform.
     */
    MANUAL("Manual Staff Check-In");

    private final String displayName;

    CheckInMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
