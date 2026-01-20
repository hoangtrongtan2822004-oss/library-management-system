package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 📊 Report Data Response DTO (Admin)
 * 
 * Dùng cho endpoint GET /api/admin/reports/data
 * 
 * 📌 Report Metrics:
 * - totalLoans: Tổng số phiếu mượn
 * - returnedLoans: Số phiếu đã trả
 * - overdueLoans: Số phiếu quá hạn
 * - totalFines: Tổng tiền phạt
 * 
 * 👉 Pattern: Read-Only Response DTO
 * - Chỉ dùng cho response (output)
 * - Tính toán từ bảng Loan và Fine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportDataDto {
    private Long totalLoans;
    private Long returnedLoans;
    private Long overdueLoans;
    private BigDecimal totalFines;
}