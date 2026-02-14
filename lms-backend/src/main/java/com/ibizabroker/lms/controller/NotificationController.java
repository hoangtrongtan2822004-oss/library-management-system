package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.NotificationDto;
import com.ibizabroker.lms.service.NotificationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Notification Controller - Basic implementation
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get user notifications
     */
    @GetMapping("/user/notifications")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        List<NotificationDto> notifications = notificationService
                .getUserNotifications(userDetails.getUsername(), limit);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/user/notifications/unread-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/user/notifications/{id}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        notificationService.markAsRead(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/user/notifications/read-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }
}
