package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a review comment
 *
 * Dùng cho: POST /api/user/reviews/{reviewId}/comments (Request)
 *
 * Validation:
 * - content: required, 1-500 chars
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewCommentRequest {

    @NotBlank(message = "Nội dung comment không được để trống")
    @Size(min = 1, max = 500, message = "Comment phải từ 1-500 ký tự")
    private String content;
}
