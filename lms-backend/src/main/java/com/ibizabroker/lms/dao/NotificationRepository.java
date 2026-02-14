package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    long countByUserUsernameAndReadFalse(String username);

    Optional<Notification> findByIdAndUserUsername(Long id, String username);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.username = :username and n.read = false")
    int markAllAsReadForUser(@Param("username") String username);
}
