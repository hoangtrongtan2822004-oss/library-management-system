package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 📦 DTO cho Public Book List APIs
 * 
 * ⚠️ QUAN TRỌNG: Tên các trường PHẢI KHỚP với Frontend Book Model
 * - Frontend tìm: id, name, coverUrl, authors (Set), categories (Set)
 * - Backend trả: Phải giống hệt để tránh undefined
 * 
 * 🎯 Lợi ích:
 * - Giảm băng thông: Chỉ trả các trường cần thiết
 * - Bảo mật: Không expose các trường nội bộ (createdBy, updatedAt...)
 * - Type-safe: authors/categories là Set<DTO> thay vì String
 * 
 * 📌 Khi nào dùng:
 * - GET /api/public/books (list view)
 * - GET /api/public/books/newest
 * - GET /api/public/books/search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookListDto {
    
    /**
     * ID sách (Frontend tìm: book.id)
     */
    private Integer id;
    
    /**
     * Tên sách (Frontend tìm: book.name)
     */
    private String name;
    
    /**
     * Danh sách tác giả (Frontend tìm: book.authors)
     */
    private Set<AuthorDto> authors;
    
    /**
     * Danh sách thể loại (Frontend tìm: book.categories)
     */
    private Set<CategoryDto> categories;
    
    /**
     * Link ảnh bìa (Frontend tìm: book.coverUrl)
     */
    private String coverUrl;
    
    /**
     * Số lượng còn lại (Frontend tìm: book.numberOfCopiesAvailable)
     */
    private Integer numberOfCopiesAvailable;
    
    /**
     * Năm xuất bản (Frontend tìm: book.publishedYear)
     */
    private Integer publishedYear;
    
    /**
     * ISBN (Frontend tìm: book.isbn)
     */
    private String isbn;
    
    /**
     * Ngày thêm vào thư viện
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
     * 🏗️ Inner DTO cho Author (Frontend tìm: authors[].name)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private String name;
    }
    
    /**
     * 🏗️ Inner DTO cho Category (Frontend tìm: categories[].name)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private String name;
    }
    
    /**
     * Chuyển đổi từ Entity sang DTO
     * ⚠️ Tên trường khớp với Frontend Book Model
     */
    public static BookListDto fromEntity(com.ibizabroker.lms.entity.Books book) {
        if (book == null) return null;
        
        return BookListDto.builder()
                .id(book.getId()) // Frontend: book.id
                .name(book.getName()) // Frontend: book.name
                .coverUrl(book.getCoverUrl()) // Frontend: book.coverUrl
                .numberOfCopiesAvailable(book.getNumberOfCopiesAvailable()) // Frontend: book.numberOfCopiesAvailable
                .publishedYear(book.getPublishedYear()) // Frontend: book.publishedYear
                .isbn(book.getIsbn()) // Frontend: book.isbn
                
                // Map Authors sang DTO con (Frontend: book.authors)
                .authors(book.getAuthors() != null ? 
                    book.getAuthors().stream()
                        .map(a -> AuthorDto.builder().name(a.getName()).build())
                        .collect(Collectors.toSet()) : null)
                        
                // Map Categories sang DTO con (Frontend: book.categories)
                .categories(book.getCategories() != null ? 
                    book.getCategories().stream()
                        .map(c -> CategoryDto.builder().name(c.getName()).build())
                        .collect(Collectors.toSet()) : null)
                
                // Optional fields
                .addedDate(book.getAddedDate())
                .averageRating(computeAverageRating(book))
                .reviewCount(computeReviewCount(book))
                .build();
    }

    private static Double computeAverageRating(com.ibizabroker.lms.entity.Books book) {
        if (book.getReviews() == null) return null;
        java.util.OptionalDouble avg = book.getReviews().stream()
            .filter(r -> r.isApproved())
            .mapToInt(r -> r.getRating())
            .average();
        return avg.isPresent() ? Double.valueOf(avg.getAsDouble()) : null;
    }

    private static Integer computeReviewCount(com.ibizabroker.lms.entity.Books book) {
        if (book.getReviews() == null) return 0;
        return (int) book.getReviews().stream().filter(r -> r.isApproved()).count();
    }
}
