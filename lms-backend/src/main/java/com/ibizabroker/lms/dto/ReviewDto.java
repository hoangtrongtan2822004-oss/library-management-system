package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ⭐ Book Review Request/Response DTO
 * 
 * Dùng cho endpoints:
 * - POST /api/user/reviews (create review)
 * - GET /api/public/books/{id}/reviews (list reviews)
 * 
 * 📌 Validation Rules (for Request):
 * - rating: Bắt buộc, 1-5 sao
 * - comment: Optional
 * - images: Optional (danh sách URLs ảnh)
 * 
 * 📌 Response Fields:
 * - Review info: id, rating, comment, images, approved, createdAt
 * - Book info: bookId, bookName
 * - User info: userId, userName
 * - Social info: likesCount, commentsCount, currentUserLiked
 * 
 * 🎯 Notes / Future improvements:
 * - Consider splitting into `ReviewRequest` and `ReviewResponse` (CQRS)
 * - Optionally add `@ValidImageUrls` and HTML sanitization for comments
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewDto {
    private Integer id;
    private Integer bookId;
    private String bookName;
    private Integer userId;
    private String userName;

    @NotNull(message = "Điểm đánh giá không được để trống.")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5.")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5.")
    private Integer rating;
    
    private String comment;
    private List<String> images;
    private Boolean approved;
    private String createdAt;
    private Long likesCount;
    private Long commentsCount;
    private Boolean currentUserLiked;
    // Book cover
    private String bookCoverUrl;
    // Admin reply
    private String adminReply;
    private String adminReplyDate;
}