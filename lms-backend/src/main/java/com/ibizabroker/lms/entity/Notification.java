package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notification Entity - User notifications system
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who receives the notification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    /**
     * Notification type: LOAN_DUE, LOAN_OVERDUE, RESERVATION_READY, etc.
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * Notification title
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Notification message content
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Is notification read?
     */
    @Column(nullable = false)
    private Boolean isRead = false;

    /**
     * Related entity ID (optional - e.g., loanId, bookId)
     */
    @Column(name = "related_id")
    private Integer relatedId;

    /**
     * Related entity type (optional - e.g., LOAN, BOOK, RESERVATION)
     */
    @Column(name = "related_type", length = 50)
    private String relatedType;

    /**
     * When notification was read
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;
}
