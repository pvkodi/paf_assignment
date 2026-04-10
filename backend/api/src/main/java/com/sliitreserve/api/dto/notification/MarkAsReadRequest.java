package com.sliitreserve.api.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * MarkAsReadRequest DTO - Request to mark notifications as read.
 *
 * Can mark single notification or batch of notifications.
 * Used by NotificationController POST /notifications/{notificationId}/read
 * or DELETE /notifications (to clear/mark all as read).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkAsReadRequest {

    /**
     * Optional list of notification IDs to mark as read.
     * If null, marks all notifications as read for user.
     */
    private List<UUID> notificationIds;
}
