package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🤖 AI Chatbot Request DTO
 * 
 * Dùng cho endpoint POST /api/chatbot/ask
 * 
 * 📌 Validation Rules:
 * - prompt: Bắt buộc, max 2000 ký tự
 * - conversationId: Optional (for conversation tracking/history)
 * 
 * 📌 Business Logic:
 * - conversationId: Nếu null → tạo conversation mới
 * - conversationId: Nếu có → tiếp tục conversation cũ (RAG context)
 * - System lưu lịch sử chat và embeddings cho RAG
 * 
 * 🎯 RAG Features:
 * - Search books in library DB
 * - Answer questions about library rules
 * - Recommend books based on user preferences
 * - Help with loan/return procedures
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequestDto {
    
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String prompt;
    
    /**
     * Conversation ID for tracking (optional)
     * - Null: New conversation
     * - Exists: Continue conversation
     */
    private String conversationId;
}