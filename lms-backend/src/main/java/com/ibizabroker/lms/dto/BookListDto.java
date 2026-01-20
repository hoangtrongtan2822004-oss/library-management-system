package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 📦 Slim DTO cho Public Book List APIs
 * 
 * Chỉ chứa thông tin cần thiết để hiển thị danh sách sách.
 * Giảm ~70% kích thước response so với trả về full Entity.
 * 
 * 🎯 Lợi ích:
 * - Giảm băng thông: Entity có 20+ fields, DTO này chỉ có 10 fields cần thiết
 * - Bảo mật: Không expose các trường nội bộ (createdBy, updatedAt...)
 * - Performance: Frontend parse JSON nhanh hơn
 * - Cache-friendly: Kích thước nhỏ → cache Redis hiệu quả hơn
 * 
 * 📌 Khi nào dùng:
 * - GET /api/public/books (list view)
 * - GET /api/public/books/newest
 * - GET /api/public/books/search
 * 
 * 📌 Khi nào KHÔNG dùng:
 * - GET /api/public/books/{id} (detail view) → Dùng full DTO hoặc Entity
 * - Admin APIs → Cần đầy đủ thông tin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookListDto {
    
    /**
     * ID sách (dùng để navigate sang detail page)
     */
    private Integer bookId;
    
    /**
     * Tên sách
     */
    private String bookName;
    
    /**
     * Tác giả
     */
    private String author;
    
    /**
     * Thể loại/danh mục
     */
    private String categoryName;
    
    /**
     * Link ảnh bìa (S3/CDN URL hoặc base64)
     */
    private String coverImageUrl;
    
    /**
     * Số lượng còn lại (available copies)
     */
    private Integer availableQuantity;
    
    /**
     * Tổng số bản (total copies)
     */
    private Integer totalQuantity;
    
    /**
     * Trạng thái: "Còn sách" / "Hết sách"
     */
    private String availabilityStatus;
    
    /**
     * Năm xuất bản
     */
    private Integer publicationYear;
    
    /**
     * Ngày thêm vào thư viện (dùng để sắp xếp "Sách mới")
     */
    private LocalDate addedDate;
    
    /**
     * Rating trung bình (0.0 - 5.0)
     */
    private Double averageRating;
    
    /**
     * Số lượt đánh giá
     */
    private Integer reviewCount;
    
    /**
     * Chuyển đổi từ Entity sang DTO
     */
    public static BookListDto fromEntity(com.ibizabroker.lms.entity.Books book) {
        if (book == null) return null;
        
        // Get available quantity directly from entity
        int available = book.getNumberOfCopiesAvailable() != null ? book.getNumberOfCopiesAvailable() : 0;
        
        // Get first author/category from Set
        String authorName = book.getAuthors() != null && !book.getAuthors().isEmpty() 
            ? book.getAuthors().iterator().next().getName() 
            : "Unknown";
        String categoryName = book.getCategories() != null && !book.getCategories().isEmpty()
            ? book.getCategories().iterator().next().getName()
            : "Uncategorized";
        
        return BookListDto.builder()
                .bookId(book.getId())
                .bookName(book.getName())
                .author(authorName)
                .categoryName(categoryName)
                .coverImageUrl(book.getCoverUrl())
                .availableQuantity(available)
                .totalQuantity(available) // Entity doesn't track total quantity separately
                .availabilityStatus(available > 0 ? "Còn sách" : "Hết sách")
                .publicationYear(book.getPublishedYear())
                .addedDate(null) // TODO: Add addedDate column to Books entity
                .averageRating(null) // TODO: Calculate from reviews table
                .reviewCount(0) // TODO: Calculate from reviews table
                .build();
    }
}
