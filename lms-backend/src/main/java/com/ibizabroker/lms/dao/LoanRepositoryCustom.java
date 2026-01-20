package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.dto.LoanStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 📊 Custom Repository Interface cho complex reporting queries
 * 
 * Tách logic báo cáo phức tạp ra khỏi LoanRepository chính để code gọn gàng hơn.
 * 
 * 📌 Vấn đề với Repository overload:
 * - LoanRepository.java có 30+ methods (CRUD + Reports + Statistics)
 * - Khó bảo trì, khó tìm method cần dùng
 * - Native queries lẫn lộn với JPQL queries
 * 
 * 📌 Giải pháp với Custom Repository:
 * - CRUD methods ở LoanRepository (JpaRepository)
 * - Reporting methods ở LoanRepositoryCustom (Custom implementation)
 * - Clear separation of concerns
 * 
 * 💡 Implementation Pattern:
 * ```
 * LoanRepository.java (interface) 
 *   extends JpaRepository, LoanRepositoryCustom
 * 
 * LoanRepositoryImpl.java (class)
 *   implements LoanRepositoryCustom
 *   uses EntityManager hoặc JdbcTemplate
 * ```
 * 
 * 🎯 Use Cases:
 * - Admin Dashboard: Charts, statistics
 * - Reports Export: Excel, PDF với custom SQL
 * - Analytics: Complex aggregations, window functions
 */
public interface LoanRepositoryCustom {

    /**
     * Lấy thống kê tổng quan cho dashboard
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Thống kê tổng hợp
     */
    LoanStatistics getDashboardStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * Thống kê mượn sách theo tháng (cho Chart.js)
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return List of {month: "2024-01", count: 150}
     */
    List<Map<String, Object>> getLoanCountsByMonth(LocalDate startDate, LocalDate endDate);

    /**
     * Top sách được mượn nhiều nhất (cho Chart.js)
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @param limit Số lượng top books
     * @return List of {bookName: "Clean Code", loanCount: 50}
     */
    List<Map<String, Object>> getMostLoanedBooks(LocalDate startDate, LocalDate endDate, int limit);

    /**
     * Thống kê phạt theo tháng (cho Chart.js)
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return List of {month: "2024-01", totalFines: 500000}
     */
    List<Map<String, Object>> getFinesByMonth(LocalDate startDate, LocalDate endDate);

    /**
     * Thống kê theo trạng thái loan (cho Pie Chart)
     * 
     * @return List of {status: "BORROWED", count: 120}
     */
    List<Map<String, Object>> countByStatus();

    /**
     * Lấy top users mượn nhiều nhất
     * 
     * @param limit Số lượng top users
     * @return List of {userId, userName, loanCount}
     */
    List<Map<String, Object>> getTopBorrowers(int limit);

    /**
     * Thống kê theo thể loại sách (Category Distribution)
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return List of {categoryName: "Technology", loanCount: 200, percentage: 35.5}
     */
    List<Map<String, Object>> getLoansByCategory(LocalDate startDate, LocalDate endDate);

    /**
     * Dead Stock Analysis - Sách không được mượn lâu
     * 
     * @param daysSinceLastLoan Số ngày kể từ lần mượn cuối
     * @return List of {bookId, bookName, lastLoanDate, daysIdle}
     */
    List<Map<String, Object>> findDeadStockBooks(int daysSinceLastLoan);

    /**
     * High Turnover Books - Sách được mượn với tỷ lệ cao
     * 
     * @param minTurnoverRate Tỷ lệ tối thiểu (loanCount / copyCount)
     * @param limit Số lượng kết quả
     * @return List of {bookId, bookName, copyCount, loanCount, turnoverRate}
     */
    List<Map<String, Object>> findHighTurnoverBooks(double minTurnoverRate, int limit);

    /**
     * Late Return Analysis - Phân tích trả trễ
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Map with {averageDaysLate, lateReturnCount, totalReturns, lateReturnRate}
     */
    Map<String, Object> getLateReturnAnalytics(LocalDate startDate, LocalDate endDate);

    /**
     * User Behavior Pattern - Phân tích hành vi người dùng
     * 
     * @param userId ID người dùng
     * @return Map with {totalLoans, onTimeReturns, lateReturns, averageLoanDuration, favoriteCategories}
     */
    Map<String, Object> getUserBehaviorPattern(Integer userId);

    /**
     * Peak Hours Analysis - Phân tích giờ cao điểm mượn sách
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return List of {hour: 14, loanCount: 50}
     */
    List<Map<String, Object>> getLoansByHourOfDay(LocalDate startDate, LocalDate endDate);

    /**
     * Revenue Forecast - Dự đoán doanh thu từ phạt
     * 
     * @param months Số tháng dự đoán
     * @return List of {month: "2024-02", predictedFines: 1500000}
     */
    List<Map<String, Object>> forecastFineRevenue(int months);
}
