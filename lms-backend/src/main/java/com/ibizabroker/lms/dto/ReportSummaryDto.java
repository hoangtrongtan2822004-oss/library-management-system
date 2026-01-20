package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 📊 Report Summary Response DTO (Admin)
 * 
 * Dùng cho endpoint GET /api/admin/reports/summary
 * 
 * 📌 Report Data:
 * - loansByMonth: Danh sách phiếu mượn theo tháng [{ month: "2025-01", count: 150 }]
 * - mostLoanedBooks: Top sách được mượn nhiều nhất [{ bookName: "...", loanCount: 50 }]
 * - finesByMonth: Tiền phạt theo tháng [{ month: "2025-01", totalFines: 500000 }]
 * 
 * 👉 Pattern: Aggregated Response DTO
 * - Chỉ dùng cho response (output)
 * - Tổng hợp nhiều nguồn dữ liệu cho báo cáo
 * - Dùng Map<String, Object> cho flexibility (các report khác nhau có fields khác nhau)
 * 
 * 🎯 TODO: Nâng cấp
 * - [ ] Tạo typed DTOs thay vì Map<String, Object>
 * - [ ] LoansByMonthEntry, MostLoanedBookEntry, FinesByMonthEntry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportSummaryDto {
    private List<Map<String, Object>> loansByMonth;
    private List<Map<String, Object>> mostLoanedBooks;
    private List<Map<String, Object>> finesByMonth;
}
