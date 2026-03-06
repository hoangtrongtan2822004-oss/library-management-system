package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_feedbacks")
public class ChatFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false)
    private Boolean helpful;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ChatFeedback() {}

    public ChatFeedback(String conversationId, String messageId, Integer userId, Boolean helpful, String reason) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.userId = userId;
        this.helpful = helpful;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Boolean getHelpful() { return helpful; }
    public void setHelpful(Boolean helpful) { this.helpful = helpful; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
