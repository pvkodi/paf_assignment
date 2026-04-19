package com.sliitreserve.api.entities.notification;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.observers.EventSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Notification Entity - User notification inbox record.
 *
 * Purpose: Stores all notifications delivered to users via multi-channel delivery system.
 * Notifications are triggered by domain events and routed by severity level.
 *
 * Design:
 * - One record per notification event (denormalized for fast inbox queries)
 * - Immutable on creation (only readAt, deliveredAt fields are mutable)
 * - Indexed on (recipientUserId, createdAt) for efficient inbox queries
 * - Indexed on createdAt for timeline sorting
 *
 * Channels (from FR-034, FR-035):
 * - IN_APP: Always set for all notifications
 * - EMAIL: Set only for HIGH severity events
 *
 * Lifecycle:
 * - Created: When event is published by observer
 * - Read: User marks as read or views notification
 * - Purged: Optionally deleted after retention period (configurable)
 *
 * Relationships:
 * - Many-to-one User (recipientUserId)
 * - Optional reference to related entity via entityReference (format: type:id)
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(columnList = "recipient_user_id, created_at DESC", name = "idx_notifications_user_timeline"),
        @Index(columnList = "recipient_user_id, read_at", name = "idx_notifications_unread"),
        @Index(columnList = "created_at DESC", name = "idx_notifications_timeline")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSeverity severity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "notification_channels",
        joinColumns = @JoinColumn(name = "notification_id")
    )
    @Column(name = "channel")
    @Enumerated(EnumType.STRING)
    private Set<NotificationChannel> channels;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "entity_reference")
    private String entityReference;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "action_label")
    private String actionLabel;

    @Column(name = "event_id", unique = true)
    private String eventId;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Check if notification has been read by recipient.
     */
    public boolean isRead() {
        return readAt != null;
    }

    /**
     * Mark notification as read at current timestamp.
     */
    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }
}
