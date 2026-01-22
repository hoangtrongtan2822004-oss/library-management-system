package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating an Ebook (admin)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbookCreateRequest {

    @NotEmpty(message = "Tiêu đề không được để trống")
    private String title;

    private String author;
    private String description;
    private String coverUrl;

    @Builder.Default
    private Boolean isPublic = true;

    @Min(value = 1, message = "Số lần download phải >= 1")
    private Integer maxDownloadsPerUser;

    /**
     * Liên kết sách vật lý (optional)
     */
    private Integer bookId;
}
