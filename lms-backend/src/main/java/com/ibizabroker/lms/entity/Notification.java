package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent notification entity for storing user notifications and read state.
 */
@Getter
@Setter
@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private NotificationType type = NotificationType.INFO;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "action_url", length = 255)
    private String actionUrl;

    @Column(name = "action_text", length = 100)
    private String actionText;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "priority")
    private Integer priority = 3;
}
