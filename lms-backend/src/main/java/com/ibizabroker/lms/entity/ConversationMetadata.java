package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores optional metadata for a chat conversation (e.g. custom title).
 * Keyed on conversationId (UUID string), so no surrogate key needed.
 */
@Entity
@Table(name = "conversation_metadata")
public class ConversationMetadata {

    @Id
    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(length = 200)
    private String title;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ConversationMetadata() {}

    public ConversationMetadata(String conversationId, Integer userId) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
