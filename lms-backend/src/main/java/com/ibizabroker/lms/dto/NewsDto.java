package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsDto {
    private Long id;
    private String title;
    private String coverImageUrl;
    private Instant publishedAt;
    private String content;
    private Boolean pinned;
    private String status;
    private Instant createdAt;
}
