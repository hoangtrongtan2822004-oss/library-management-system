package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Notification;
import com.ibizabroker.lms.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Notification Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find notifications by user, ordered by creation date descending
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(Users user, Pageable pageable);

    /**
     * Find unread notifications by user
     */
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(Users user);

    /**
     * Count unread notifications for user
     */
    long countByUserAndIsReadFalse(Users user);

    /**
     * Mark all user notifications as read
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadByUser(@Param("user") Users user);

    /**
     * Delete old read notifications (cleanup)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.isRead = true AND n.readAt < :beforeDate")
    void deleteOldReadNotifications(@Param("user") Users user, @Param("beforeDate") java.time.LocalDateTime beforeDate);
}
