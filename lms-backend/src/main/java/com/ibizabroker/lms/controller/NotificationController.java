package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.Notification;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification Controller - Full implementation
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UsersRepository usersRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Get user notifications
     */
    @GetMapping("/user/notifications")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        
        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Page<Notification> notificationPage = notificationService.getUserNotifications(user, limit);
        List<Map<String, Object>> notifications = new ArrayList<>();

        for (Notification notification : notificationPage.getContent()) {
            Map<String, Object> notifMap = new HashMap<>();
            notifMap.put("id", notification.getId());
            notifMap.put("type", notification.getType());
            notifMap.put("title", notification.getTitle());
            notifMap.put("message", notification.getMessage());
            notifMap.put("isRead", notification.getIsRead());
            notifMap.put("createdAt", notification.getCreatedAt().format(DATE_FORMATTER));
            
            if (notification.getRelatedId() != null) {
                notifMap.put("relatedId", notification.getRelatedId());
                notifMap.put("relatedType", notification.getRelatedType());
            }
            
            if (notification.getReadAt() != null) {
                notifMap.put("readAt", notification.getReadAt().format(DATE_FORMATTER));
            }
            
            notifications.add(notifMap);
        }
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/user/notifications/unread-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        long count = notificationService.getUnreadCount(user);
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
        
        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        notificationService.markAsRead(user, id);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu là đã đọc"));
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/user/notifications/read-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        Users user = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả là đã đọc"));
    }
}
