package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 📊 Dashboard Statistics Response DTO (Admin)
 * 
 * Dùng cho endpoint GET /api/admin/dashboard/stats
 * 
 * 📌 Metrics:
 * - totalBooks: Tổng số đầu sách trong hệ thống
 * - totalUsers: Tổng số người dùng
 * - activeLoans: Số phiếu mượn đang hoạt động (chưa trả)
 * - overdueLoans: Số phiếu mượn quá hạn
 * - totalFines: Tổng tiền phạt (tất cả)
 * - totalUnpaidFines: Tổng tiền phạt chưa thanh toán
 * 
 * 👉 Pattern: Read-Only Response DTO
 * - Chỉ dùng cho response (output)
 * - Tính toán từ nhiều bảng (Books, Users, Loans, Fines)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsDto {
    private Long totalBooks;
    private Long totalUsers;
    private Long activeLoans;
    private Long overdueLoans;
    private BigDecimal totalFines;
    private BigDecimal totalUnpaidFines;
}