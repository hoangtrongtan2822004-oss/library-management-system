package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 💬 Review Comment DTO (Hybrid - Request & Response)
 * 
 * Dùng cho:
 * - POST /api/user/reviews/{reviewId}/comments (Request)
 * - GET /api/public/reviews/{reviewId}/comments (Response)
 * 
 * 📌 Validation Rules (Request):
 * - content: Bắt buộc, 1-500 ký tự
 * 
 * 📌 Response Fields:
 * - id, reviewId, userId, userName, content, createdAt
 * 
 * 🎯 TODO: CQRS Pattern
 * - [ ] Tách ReviewCommentRequest (input) vs ReviewCommentResponse (output)
 * - [ ] ReviewCommentRequest: chỉ có content field
 * - [ ] ReviewCommentResponse: có đầy đủ id, userName, createdAt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewCommentDto {
    
    /**
     * Comment ID (response only)
     */
    private Integer id;
    
    /**
     * Review ID (response only)
     */
    private Integer reviewId;
    
    /**
     * User ID (response only)
     */
    private Integer userId;
    
    /**
     * User name (response only)
     */
    private String userName;
    
    /**
     * Comment content (request + response)
     */
    @NotBlank(message = "Nội dung comment không được để trống")
    @Size(min = 1, max = 500, message = "Comment phải từ 1-500 ký tự")
    private String content;
    
    /**
     * Created timestamp (response only)
     */
    private String createdAt;
}
