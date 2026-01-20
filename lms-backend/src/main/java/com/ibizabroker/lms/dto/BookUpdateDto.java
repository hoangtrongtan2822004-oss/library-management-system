package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ibizabroker.lms.validation.ValidISBN;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 📝 Book Update Request DTO
 * 
 * Dùng cho endpoint PUT/PATCH /api/admin/books/{id}
 * 
 * 📌 Khác biệt so với BookCreateDto:
 * - Tất cả fields đều optional (cho phép partial update)
 * - numberOfCopiesAvailable cho phép = 0
 * 
 * 👉 Pattern: Partial Update (PATCH semantic)
 * - Chỉ update các field không null
 * - Không cần gửi full object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookUpdateDto {
    
    @Size(min = 1, message = "Tên sách phải có ít nhất 1 ký tự")
    private String name;
    
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer numberOfCopiesAvailable;
    
    @Min(value = 1000, message = "Năm xuất bản phải >= 1000")
    private Integer publishedYear;
    
    @ValidISBN
    private String isbn;
    
    private String coverUrl;
    private Set<Integer> authorIds;
    private Set<Integer> categoryIds;
}