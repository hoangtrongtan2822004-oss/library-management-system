package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 💰 Fine Details Response DTO
 * 
 * Dùng cho endpoint GET /api/admin/fines hoặc GET /api/user/my-fines
 * 
 * 📌 Fine Fields:
 * - loanId: ID phiếu mượn
 * - bookName: Tên sách
 * - userName: Tên người mượn
 * - dueDate: Hạn trả
 * - returnDate: Ngày trả thực tế
 * - overdueDays: Số ngày quá hạn (auto-calculated)
 * - fineAmount: Số tiền phạt
 * 
 * 👉 Pattern: Response-Only DTO with Calculation
 * - Chỉ dùng cho response (output)
 * - overdueDays được tự động tính trong constructor
 * - Hỗ trợ JPA constructor expression query
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FineDetailsDto {
    private Integer loanId;
    private String bookName;
    private String userName;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Long overdueDays;
    private BigDecimal fineAmount;
    
    /**
     * Constructor đặc biệt cho JPA Query (không có overdueDays)
     * Tự động tính overdueDays từ dueDate và returnDate
     */
    public FineDetailsDto(Integer loanId, String bookName, String userName, 
                         LocalDate dueDate, LocalDate returnDate, BigDecimal fineAmount) {
        this.loanId = loanId;
        this.bookName = bookName;
        this.userName = userName;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        
        // Auto-calculate overdueDays
        if (dueDate != null && returnDate != null && returnDate.isAfter(dueDate)) {
            this.overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        } else {
            this.overdueDays = 0L;
        }
    }
}