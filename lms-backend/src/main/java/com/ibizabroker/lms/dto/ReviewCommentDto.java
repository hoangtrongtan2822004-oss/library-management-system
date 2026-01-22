package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 💬 Review Comment DTO (Hybrid - Request & Response)
 * 
 * Response DTO for review comments
 *
 * Dùng cho: GET /api/public/reviews/{reviewId}/comments (Response)
 *
 * 📌 Response Fields:
 * - id, reviewId, userId, userName, content, createdAt
 *
 * Note: request validation has been moved to `ReviewCommentRequest`.
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
     * Comment content (response)
     */
    private String content;
    
    /**
     * Created timestamp (response only)
     */
    private String createdAt;
}
