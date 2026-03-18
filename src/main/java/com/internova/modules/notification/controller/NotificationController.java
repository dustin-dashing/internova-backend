package com.internova.modules.notification.controller;

import com.internova.core.model.User;
import com.internova.modules.notification.dto.NotificationResponse;
import com.internova.modules.notification.model.Notification;
import com.internova.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for authenticated user
     * GET /api/v1/notifications
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getUserNotifications(user.getId(), pageable);

        List<NotificationResponse> responses = notifications.getContent().stream()
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getIsRead(),
                        n.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "notifications", responses,
                "totalPages", notifications.getTotalPages(),
                "totalElements", notifications.getTotalElements(),
                "unreadCount", notificationService.getUnreadCount(user.getId())));
    }

    /**
     * Get unread notifications count
     * GET /api/v1/notifications/unread/count
     */
    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark notification as read
     * PATCH /api/v1/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        Notification notification = notificationService.getNotificationById(id);

        // Verify ownership
        if (!notification.getRecipient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    /**
     * Mark all notifications as read
     * PATCH /api/v1/notifications/read-all
     */
    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    /**
     * Delete notification
     * DELETE /api/v1/notifications/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        Notification notification = notificationService.getNotificationById(id);

        // Verify ownership
        if (!notification.getRecipient().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    /**
     * Delete all notifications
     * DELETE /api/v1/notifications
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteAllNotifications(@AuthenticationPrincipal User user) {
        notificationService.deleteAllNotifications(user.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications deleted"));
    }
}
