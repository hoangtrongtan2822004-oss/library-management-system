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
 * 📋 Loan Details Response DTO
 * 
 * Dùng cho endpoint GET /api/user/loans (và admin endpoints)
 * 
 * 📌 JPA Projection Pattern:
 * - Constructor khớp với câu truy vấn trong LoanRepository
 * - @AllArgsConstructor cho phép JPA instantiate trực tiếp
 * 
 * 📌 Fields:
 * - Loan info: loanId, loanDate, dueDate, returnDate, status
 * - Book info: bookName
 * - User info: userName
 * - Fine info: fineAmount, overdueDays
 * 
 * 👉 Pattern: Read-Only Response DTO
 * - Chỉ dùng cho response (output)
 * - KHÔNG BAO GIỞ dùng cho request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanDetailsDto {
    private Integer loanId;
    private String bookName;
    private String userName;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;
    private BigDecimal fineAmount;
    private Long overdueDays;
}