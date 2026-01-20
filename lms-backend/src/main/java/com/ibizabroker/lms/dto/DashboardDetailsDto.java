package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ibizabroker.lms.entity.Loan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 📊 Dashboard Details Response DTO (Admin)
 * 
 * Dùng cho endpoint GET /api/admin/dashboard
 * 
 * 📌 Aggregates:
 * - stats: Thống kê tổng quan (LoanStatistics hoặc DashboardStatsDto)
 * - mostLoanedBooks: Top sách được mượn nhiều nhất
 * - topBorrowers: Top người mượn nhiều nhất
 * - recentActivities: Hoạt động mới nhất
 * - overdueLoans: Danh sách phiếu mượn quá hạn
 * 
 * 👉 Pattern: Dashboard Aggregator DTO
 * - Tổng hợp nhiều nguồn dữ liệu
 * - Chỉ dùng cho response (output)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardDetailsDto {
    private DashboardStatsDto stats;
    private List<Map<String, Object>> mostLoanedBooks;
    private List<Map<String, Object>> topBorrowers;
    private List<Loan> recentActivities;
    private List<Loan> overdueLoans;
}
