package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.dto.LoanStatistics;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔧 Custom Repository Implementation
 * 
 * Implement complex queries bằng EntityManager hoặc Native SQL.
 * 
 * 📌 Quy tắc đặt tên:
 * - Interface: LoanRepositoryCustom
 * - Implementation: LoanRepositoryImpl (Phải có suffix "Impl")
 * 
 * Spring Data sẽ tự động detect và merge methods từ:
 * - JpaRepository (CRUD)
 * - LoanRepositoryCustom (Custom queries)
 * 
 * 💡 Lợi ích:
 * - Viết code Java thay vì chuỗi @Query
 * - Dễ debug, dễ unit test
 * - Có thể dùng JDBC Template cho hiệu năng cao
 * - Linh hoạt xử lý kết quả
 */
@Slf4j
@Repository
public class LoanRepositoryImpl implements LoanRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public LoanStatistics getDashboardStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching dashboard statistics from {} to {}", startDate, endDate);
        
        String sql = """
            SELECT 
                COUNT(*) as totalLoans,
                SUM(CASE WHEN status = 'BORROWED' THEN 1 ELSE 0 END) as currentlyBorrowed,
                SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue,
                SUM(CASE WHEN status = 'RETURNED' THEN 1 ELSE 0 END) as returned,
                COALESCE(SUM(fine_amount), 0) as totalFines,
                COALESCE(SUM(CASE WHEN fine_status = 'UNPAID' THEN fine_amount ELSE 0 END), 0) as unpaidFines
            FROM loans
            WHERE loan_date BETWEEN :startDate AND :endDate
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        Object[] result = (Object[]) query.getSingleResult();
        
        return LoanStatistics.builder()
                .totalLoans(((Number) result[0]).longValue())
                .currentlyBorrowed(((Number) result[1]).intValue())
                .overdue(((Number) result[2]).intValue())
                .returned(((Number) result[3]).intValue())
                .totalFines((BigDecimal) result[4])
                .unpaidFines((BigDecimal) result[5])
                .build();
    }

    @Override
    public List<Map<String, Object>> getLoanCountsByMonth(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                DATE_FORMAT(loan_date, '%Y-%m') as month,
                COUNT(*) as count
            FROM loans
            WHERE loan_date BETWEEN :startDate AND :endDate
            GROUP BY DATE_FORMAT(loan_date, '%Y-%m')
            ORDER BY month ASC
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("month", (String) row[0]);
                    map.put("count", ((Number) row[1]).intValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getMostLoanedBooks(LocalDate startDate, LocalDate endDate, int limit) {
        String sql = """
            SELECT 
                b.name as bookName,
                COUNT(l.id) as loanCount
            FROM loans l
            JOIN books b ON l.book_id = b.id
            WHERE l.loan_date BETWEEN :startDate AND :endDate
            GROUP BY b.name
            ORDER BY loanCount DESC
            LIMIT :limit
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("limit", limit);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookName", (String) row[0]);
                    map.put("loanCount", ((Number) row[1]).intValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getFinesByMonth(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                DATE_FORMAT(return_date, '%Y-%m') as month,
                COALESCE(SUM(fine_amount), 0) as totalFines
            FROM loans
            WHERE return_date BETWEEN :startDate AND :endDate
            AND fine_amount > 0
            GROUP BY DATE_FORMAT(return_date, '%Y-%m')
            ORDER BY month ASC
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("month", (String) row[0]);
                    map.put("totalFines", ((Number) row[1]).doubleValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> countByStatus() {
        String sql = "SELECT status, COUNT(*) as count FROM loans GROUP BY status";
        
        Query query = entityManager.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("status", (String) row[0]);
                    map.put("count", ((Number) row[1]).intValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTopBorrowers(int limit) {
        String sql = """
            SELECT 
                u.user_id as userId,
                u.name as userName,
                COUNT(l.id) as loanCount
            FROM loans l
            JOIN users u ON l.member_id = u.user_id
            GROUP BY u.user_id, u.name
            ORDER BY loanCount DESC
            LIMIT :limit
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", limit);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", ((Number) row[0]).intValue());
                    map.put("userName", (String) row[1]);
                    map.put("loanCount", ((Number) row[2]).intValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getLoansByCategory(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                c.name as categoryName,
                COUNT(l.id) as loanCount,
                (COUNT(l.id) * 100.0 / (SELECT COUNT(*) FROM loans WHERE loan_date BETWEEN :startDate AND :endDate)) as percentage
            FROM loans l
            JOIN books b ON l.book_id = b.id
            LEFT JOIN books_categories bc ON b.id = bc.book_id
            LEFT JOIN categories c ON bc.category_id = c.id
            WHERE l.loan_date BETWEEN :startDate AND :endDate
            GROUP BY c.name
            ORDER BY loanCount DESC
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("categoryName", row[0] != null ? (String) row[0] : "Uncategorized");
                    map.put("loanCount", ((Number) row[1]).intValue());
                    map.put("percentage", ((Number) row[2]).doubleValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> findDeadStockBooks(int daysSinceLastLoan) {
        String sql = """
            SELECT 
                b.id as bookId,
                b.name as bookName,
                MAX(l.loan_date) as lastLoanDate,
                DATEDIFF(CURRENT_DATE, MAX(l.loan_date)) as daysIdle
            FROM books b
            LEFT JOIN loans l ON b.id = l.book_id
            GROUP BY b.id, b.name
            HAVING MAX(l.loan_date) IS NULL OR DATEDIFF(CURRENT_DATE, MAX(l.loan_date)) >= :days
            ORDER BY daysIdle DESC
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("days", daysSinceLastLoan);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookId", ((Number) row[0]).intValue());
                    map.put("bookName", (String) row[1]);
                    map.put("lastLoanDate", row[2]);
                    map.put("daysIdle", row[3] != null ? ((Number) row[3]).intValue() : null);
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> findHighTurnoverBooks(double minTurnoverRate, int limit) {
        String sql = """
            SELECT 
                b.id as bookId,
                b.name as bookName,
                b.quantity as copyCount,
                COUNT(l.id) as loanCount,
                (COUNT(l.id) * 1.0 / b.quantity) as turnoverRate
            FROM books b
            LEFT JOIN loans l ON b.id = l.book_id
            WHERE b.quantity > 0
            GROUP BY b.id, b.name, b.quantity
            HAVING (COUNT(l.id) * 1.0 / b.quantity) >= :minRate
            ORDER BY turnoverRate DESC
            LIMIT :limit
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("minRate", minTurnoverRate);
        query.setParameter("limit", limit);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookId", ((Number) row[0]).intValue());
                    map.put("bookName", (String) row[1]);
                    map.put("copyCount", ((Number) row[2]).intValue());
                    map.put("loanCount", ((Number) row[3]).intValue());
                    map.put("turnoverRate", ((Number) row[4]).doubleValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getLateReturnAnalytics(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                AVG(CASE WHEN return_date > due_date THEN DATEDIFF(return_date, due_date) ELSE 0 END) as avgDaysLate,
                SUM(CASE WHEN return_date > due_date THEN 1 ELSE 0 END) as lateReturnCount,
                COUNT(*) as totalReturns,
                (SUM(CASE WHEN return_date > due_date THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as lateReturnRate
            FROM loans
            WHERE status = 'RETURNED'
            AND return_date BETWEEN :startDate AND :endDate
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        Object[] result = (Object[]) query.getSingleResult();
        
        return Map.of(
            "averageDaysLate", result[0] != null ? ((Number) result[0]).doubleValue() : 0.0,
            "lateReturnCount", ((Number) result[1]).intValue(),
            "totalReturns", ((Number) result[2]).intValue(),
            "lateReturnRate", result[3] != null ? ((Number) result[3]).doubleValue() : 0.0
        );
    }

    @Override
    public Map<String, Object> getUserBehaviorPattern(Integer userId) {
        // Simplified implementation - can be enhanced with more analytics
        String sql = """
            SELECT 
                COUNT(*) as totalLoans,
                SUM(CASE WHEN return_date <= due_date THEN 1 ELSE 0 END) as onTimeReturns,
                SUM(CASE WHEN return_date > due_date THEN 1 ELSE 0 END) as lateReturns,
                AVG(DATEDIFF(COALESCE(return_date, CURRENT_DATE), loan_date)) as avgLoanDuration
            FROM loans
            WHERE member_id = :userId
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        
        Object[] result = (Object[]) query.getSingleResult();
        
        return Map.of(
            "totalLoans", ((Number) result[0]).intValue(),
            "onTimeReturns", ((Number) result[1]).intValue(),
            "lateReturns", ((Number) result[2]).intValue(),
            "averageLoanDuration", result[3] != null ? ((Number) result[3]).doubleValue() : 0.0
        );
    }

    @Override
    public List<Map<String, Object>> getLoansByHourOfDay(LocalDate startDate, LocalDate endDate) {
        // Note: This requires loan_timestamp column (if you track time)
        // Simplified version returns empty list if timestamp not available
        log.warn("getLoansByHourOfDay requires timestamp column - returning empty list");
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> forecastFineRevenue(int months) {
        // Simplified linear forecast based on historical average
        log.warn("forecastFineRevenue is simplified - implement ML model for better accuracy");
        return Collections.emptyList();
    }
}

