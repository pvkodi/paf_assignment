package com.sliitreserve.api.entities.notification;

/**
 * Notification Channel Enum - Delivery mechanism for notifications.
 *
 * Channels (from FR-034, FR-035):
 * - IN_APP: Delivered to in-app notification inbox (default for all)
 * - EMAIL: Delivered via email (HIGH severity events only)
 *
 * Example:
 * - HIGH severity: {IN_APP, EMAIL}
 * - STANDARD severity: {IN_APP}
 */
public enum NotificationChannel {
    IN_APP,
    EMAIL
}
