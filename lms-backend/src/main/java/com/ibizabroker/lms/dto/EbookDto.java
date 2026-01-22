package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📚 E-Book DTO (Hybrid - Request & Response)
 * 
 * Dùng cho:
 * - POST /api/admin/ebooks (Request)
 * - GET /api/public/ebooks (Response)
 * 
 * 📌 Validation Rules (Request):
 * - title: Bắt buộc
 * - author: Optional
 * - maxDownloadsPerUser: Optional, >= 1 nếu có
 * 
 * 📌 Business Logic:
 * - isPublic: true = Ai cũng download được, false = Chỉ member
 * - bookId: Liên kết với sách vật lý (optional)
 * - maxDownloadsPerUser: Giới hạn download mỗi user
 * 
 * 🎯 Status: Split implemented
 * - Use `EbookCreateRequest` for incoming create/update payloads
 * - Use `EbookResponse` for returned ebook data (includes fileSize/format/downloadCount)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Deprecated
public class EbookDto {

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
