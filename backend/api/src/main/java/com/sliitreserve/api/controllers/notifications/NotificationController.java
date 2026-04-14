package com.sliitreserve.api.controllers.notifications;

import com.sliitreserve.api.dto.notification.MarkAsReadRequest;
import com.sliitreserve.api.dto.notification.NotificationResponse;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.notification.NotificationRepository;
import com.sliitreserve.api.repositories.auth.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        log.info("Fetching notifications for user: {}", authentication.getName());

        try {
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));

            int pageSize = Math.min(size, 100);
            Pageable pageable = PageRequest.of(page, pageSize);

            Page<NotificationResponse> notifications = notificationRepository
                .findByRecipientUser_IdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToResponse);

            log.debug("Retrieved {} notifications for user {}", notifications.getSize(), user.getId());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error fetching notifications for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<NotificationResponse> getNotification(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        log.info("Fetching notification {} for user: {}", id, authentication.getName());

        try {
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));

            var notification = notificationRepository.findById(id)
                .filter(n -> n.getRecipientUser().getId().equals(user.getId()))
                .map(this::mapToResponse)
                .orElse(null);

            if (notification == null) {
                log.warn("Notification {} not found or user {} not authorized", id, user.getId());
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            log.error("Error fetching notification: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<NotificationResponse> markAsRead(
        @PathVariable UUID id,
        Authentication authentication
    ) {
        log.info("Marking notification {} as read for user: {}", id, authentication.getName());

        try {
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));

            var notification = notificationRepository.findById(id)
                .filter(n -> n.getRecipientUser().getId().equals(user.getId()))
                .orElse(null);

            if (notification == null) {
                log.warn("Notification {} not found or user {} not authorized", id, user.getId());
                return ResponseEntity.notFound().build();
            }

            notification.markAsRead();
            var saved = notificationRepository.save(notification);

            log.debug("Marked notification {} as read for user {}", id, user.getId());
            return ResponseEntity.ok(mapToResponse(saved));
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        log.info("Marking all notifications as read for user: {}", authentication.getName());

        try {
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));

            List<com.sliitreserve.api.entities.notification.Notification> unread = notificationRepository.findUnreadByUserId(user.getId());
            unread.forEach(com.sliitreserve.api.entities.notification.Notification::markAsRead);
            notificationRepository.saveAll(unread);

            log.debug("Marked {} notifications as read for user {}", unread.size(), user.getId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error marking all notifications as read for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'TECHNICIAN', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        log.info("Fetching unread notification count for user: {}", authentication.getName());

        try {
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));

            long unreadCount = notificationRepository.countByRecipientUser_IdAndReadAtIsNull(user.getId());

            log.debug("User {} has {} unread notifications", user.getId(), unreadCount);
            return ResponseEntity.ok(java.util.Map.of("unreadCount", unreadCount));
        } catch (Exception e) {
            log.error("Error fetching unread count for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private NotificationResponse mapToResponse(com.sliitreserve.api.entities.notification.Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .eventType(notification.getEventType())
            .severity(notification.getSeverity())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .entityReference(notification.getEntityReference())
            .actionUrl(notification.getActionUrl())
            .actionLabel(notification.getActionLabel())
            .read(notification.isRead())
            .deliveredAt(notification.getDeliveredAt())
            .readAt(notification.getReadAt())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
