package com.sliitreserve.api.dto.notification;

import com.sliitreserve.api.observers.EventSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * NotificationResponse DTO - Notification inbox item response.
 *
 * Used by NotificationController to return notification to client.
 * Excludes internal fields (channels as enum, raw eventId).
 * Includes: id, title, message, severity, read status, timestamps, action link.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;

    private String eventType;

    private EventSeverity severity;

    private String title;

    private String message;

    private String entityReference;

    private String actionUrl;

    private String actionLabel;

    private boolean read;

    private LocalDateTime deliveredAt;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
