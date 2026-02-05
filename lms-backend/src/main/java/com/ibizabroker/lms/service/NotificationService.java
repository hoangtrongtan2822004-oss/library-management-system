package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.NotificationRepository;
import com.ibizabroker.lms.entity.Notification;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Notification Service - Business logic for notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Get paginated notifications for user
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Users user, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get unread notification count for user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Users user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Users user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        // Security check: ensure notification belongs to user (null-safe)
        if (!Objects.equals(notification.getUser().getUserId(), user.getUserId())) {
            throw new NotFoundException("Notification not found");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for user
     */
    @Transactional
    public void markAllAsRead(Users user) {
        notificationRepository.markAllAsReadByUser(user);
    }

    /**
     * Create notification for user
     */
    @Transactional
    public Notification createNotification(Users user, String type, String title, String message,
                                           Integer relatedId, String relatedType) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);

        return notificationRepository.save(notification);
    }

    /**
     * Delete old read notifications (cleanup task)
     */
    @Transactional
    public void cleanupOldNotifications(Users user, int daysOld) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteOldReadNotifications(user, beforeDate);
    }
}
