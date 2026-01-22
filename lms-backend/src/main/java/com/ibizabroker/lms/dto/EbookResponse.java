package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Ebook (public consumption)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EbookResponse {
    private Long id;
    private Integer bookId;
    private String title;
    private String author;
    private String description;
    private String coverUrl;
    private Boolean isPublic;
    private Integer maxDownloadsPerUser;
    private String format; // PDF, EPUB
    private Long fileSize;
    private Integer downloadCount;
}
