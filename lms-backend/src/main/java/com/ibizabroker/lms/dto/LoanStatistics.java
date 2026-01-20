package com.ibizabroker.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 📊 DTO cho thống kê tổng quan Dashboard
 * 
 * Dùng cho LoanRepositoryCustom.getDashboardStatistics()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatistics {
    private Long totalLoans;
    private Integer currentlyBorrowed;
    private Integer overdue;
    private Integer returned;
    private BigDecimal totalFines;
    private BigDecimal unpaidFines;
    
    /**
     * Tỷ lệ hoàn trả đúng hạn (%)
     */
    public double getOnTimeReturnRate() {
        if (totalLoans == 0) return 0.0;
        return (returned * 100.0) / totalLoans;
    }
    
    /**
     * Tỷ lệ quá hạn (%)
     */
    public double getOverdueRate() {
        if (totalLoans == 0) return 0.0;
        return (overdue * 100.0) / totalLoans;
    }
}
