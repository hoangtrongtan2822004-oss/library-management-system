package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📚 Loan Creation Request DTO
 * 
 * Dùng cho endpoint POST /api/user/borrow
 * 
 * 📌 Validation Rules:
 * - bookId: Bắt buộc
 * - memberId: Bắt buộc
 * - loanDays: Optional, default 14 ngày, >= 1
 * - quantity: Optional, default 1, >= 1
 * - studentName: Optional (cho mục đích đối chiếu)
 * - studentClass: Optional
 * 
 * 🎯 Business Rules:
 * - Hệ thống sẽ tự động check số lượng sách còn tồn
 * - dueDate = loanDate + loanDays
 * - Lưu ý: 1 user có thể mượn nhiều cuốn cùng 1 lúc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanRequest {
    @NotNull(message = "Book ID is required")
    private Integer bookId;
    
    @NotNull(message = "Member ID is required")
    private Integer memberId;
    
    @Builder.Default
    @Min(value = 1, message = "Loan days must be at least 1")
    private Integer loanDays = 14;

    @Builder.Default
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;
    
    private String studentName;
    private String studentClass;
}
