package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.NotificationRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.NotificationDto;
import com.ibizabroker.lms.entity.Notification;
import com.ibizabroker.lms.entity.NotificationType;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UsersRepository usersRepository;

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(String username, int limit) {
        int pageSize = Math.max(1, limit);
        return notificationRepository
                .findByUserUsernameOrderByCreatedAtDesc(username, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByUserUsernameAndReadFalse(username);
    }

    @Transactional
    public void markAsRead(Long id, String username) {
        Notification notification = notificationRepository
                .findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new NotFoundException("Notification not found", "NOTIFICATION_NOT_FOUND"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public int markAllAsRead(String username) {
        return notificationRepository.markAllAsReadForUser(username);
    }

    @Transactional
    public NotificationDto createNotification(String username, NotificationDto payload) {
        Users user = usersRepository
                .findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(payload.getType() != null ? payload.getType() : NotificationType.INFO);
        notification.setTitle(payload.getTitle());
        notification.setMessage(payload.getMessage());
        notification.setActionUrl(payload.getActionUrl());
        notification.setActionText(payload.getActionText());
        notification.setRead(Boolean.TRUE.equals(payload.getIsRead()));
        notification.setPriority(payload.getPriority() != null ? payload.getPriority() : 3);

        Notification saved = notificationRepository.save(notification);
        return toDto(saved);
    }

    private NotificationDto toDto(Notification notification) {
        LocalDateTime timestamp = notification.getCreatedAt() != null
                ? notification.getCreatedAt()
                : LocalDateTime.now();

        return NotificationDto.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .actionUrl(notification.getActionUrl())
                .actionText(notification.getActionText())
                .timestamp(timestamp)
                .isRead(notification.isRead())
                .priority(notification.getPriority())
                .build();
    }
}
