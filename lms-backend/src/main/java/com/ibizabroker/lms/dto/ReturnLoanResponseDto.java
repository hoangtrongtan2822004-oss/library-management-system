package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ibizabroker.lms.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 📤 Return Loan Response DTO
 * 
 * Dùng cho endpoint POST /api/user/loans/return hoặc POST /api/admin/loans/{id}/return
 * 
 * 📌 Response Info:
 * - loanId, bookId, memberId: IDs
 * - loanDate, dueDate, returnDate: Dates
 * - status: LoanStatus (RETURNED)
 * - fineAmount: Số tiền phạt (nếu quá hạn)
 * - overdueDays: Số ngày quá hạn
 * 
 * 👉 Pattern: Response-Only DTO
 * - Chỉ dùng cho response (output)
 * - Tính toán từ Loan entity + Fine logic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReturnLoanResponseDto {
    private Integer loanId;
    private Integer bookId;
    private Integer memberId;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;
    private BigDecimal fineAmount;
    private Long overdueDays;

    public Integer getLoanId() { return loanId; }
    public Integer getBookId() { return bookId; }
    public Integer getMemberId() { return memberId; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public LoanStatus getStatus() { return status; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public Long getOverdueDays() { return overdueDays; }
}
