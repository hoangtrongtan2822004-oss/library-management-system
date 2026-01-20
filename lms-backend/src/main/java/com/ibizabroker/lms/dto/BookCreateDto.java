package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ibizabroker.lms.validation.ValidISBN;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 📚 Book Create Request DTO
 * 
 * Dùng cho endpoint POST /api/admin/books
 * 
 * 📌 Validation Rules:
 * - name: Bắt buộc, min 1 ký tự
 * - numberOfCopiesAvailable: Bắt buộc, >= 1
 * - publishedYear: Optional, nếu có thì >= 1000
 * - isbn: Optional, nếu có thì phải là 10/13 chữ số VÀ checksum đúng ✅
 * - authorIds: Bắt buộc, ít nhất 1 tác giả
 * - categoryIds: Bắt buộc, ít nhất 1 thể loại
 * 
 * 🎯 TODO: Nâng cấp thêm
 * - [ ] @ValidPublishedYear (không được quá năm hiện tại)
 * - [ ] @ExistsInDatabase cho authorIds và categoryIds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookCreateDto {

    @NotEmpty(message = "Tên sách không được để trống")
    @Size(min = 1, message = "Tên sách phải có ít nhất 1 ký tự")
    private String name;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer numberOfCopiesAvailable;

    @Min(value = 1000, message = "Năm xuất bản phải >= 1000")
    private Integer publishedYear;
    
    @ValidISBN
    private String isbn;
    
    private String coverUrl;

    @NotNull(message = "Tác giả không được để trống")
    @Size(min = 1, message = "Phải có ít nhất 1 ID tác giả")
    private Set<Integer> authorIds;

    @NotNull(message = "Thể loại không được để trống")
    @Size(min = 1, message = "Phải có ít nhất 1 ID thể loại")
    private Set<Integer> categoryIds;
}