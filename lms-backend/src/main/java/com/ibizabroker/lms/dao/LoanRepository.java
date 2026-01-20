package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.dto.FineDetailsDto;
import com.ibizabroker.lms.dto.LoanDetailsDto;
import com.ibizabroker.lms.entity.Loan;
import com.ibizabroker.lms.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 📚 Loan Repository
 * 
 * ✅ CRUD methods (inherited từ JpaRepository)
 * ✅ Simple query methods (Spring Data auto-generate)
 * ✅ Custom reporting methods (từ LoanRepositoryCustom)
 * 
 * 📌 Custom Repository Pattern:
 * - LoanRepository: CRUD + simple queries
 * - LoanRepositoryCustom: Complex reports, analytics
 * - LoanRepositoryImpl: Implementation với EntityManager
 */
public interface LoanRepository extends JpaRepository<Loan, Integer>, LoanRepositoryCustom {

    Page<Loan> findByMember_UserId(Integer memberId, Pageable pageable);
    List<Loan> findByMember_UserId(Integer memberId);
    List<Loan> findByMember_UserIdAndStatus(Integer memberId, LoanStatus status);
    List<Loan> findByBook_Id(Integer bookId);
    
    // Alias methods for easier usage
    default List<Loan> findByMemberId(Integer memberId) {
        return findByMember_UserId(memberId);
    }
    
    default List<Loan> findByMemberIdAndStatus(Integer memberId, LoanStatus status) {
        return findByMember_UserIdAndStatus(memberId, status);
    }
    
    default List<Loan> findByBookId(Integer bookId) {
        return findByBook_Id(bookId);
    }
    long countByStatus(LoanStatus status);
    List<Loan> findTop5ByOrderByLoanDateDesc();
    List<Loan> findByStatus(LoanStatus status);

    @Query("SELECT new com.ibizabroker.lms.dto.LoanDetailsDto(" +
           "l.id, b.name, u.name, l.loanDate, l.dueDate, l.returnDate, l.status, " +
           "l.fineAmount, " +
           "CAST(" +
           "  CASE " +
           "    WHEN l.returnDate IS NOT NULL AND l.returnDate > l.dueDate " +
           "      THEN (l.returnDate - l.dueDate) " +
           "    WHEN l.status = com.ibizabroker.lms.entity.LoanStatus.OVERDUE " +
           "      THEN (CURRENT_DATE - l.dueDate) " +
           "    ELSE 0 " +
           "  END AS long" +
           ") ) " +
           "FROM Loan l JOIN l.book b JOIN l.member u " +
           "ORDER BY l.loanDate DESC")
    List<LoanDetailsDto> findAllLoanDetails();

    @Query("SELECT new com.ibizabroker.lms.dto.LoanDetailsDto(" +
           "l.id, b.name, u.name, l.loanDate, l.dueDate, l.returnDate, l.status, " +
           "l.fineAmount, " +
           "CAST(" +
           "  CASE " +
           "    WHEN l.returnDate IS NOT NULL AND l.returnDate > l.dueDate " +
           "      THEN (l.returnDate - l.dueDate) " +
           "    WHEN l.status = com.ibizabroker.lms.entity.LoanStatus.OVERDUE " +
           "      THEN (CURRENT_DATE - l.dueDate) " +
           "    ELSE 0 " +
           "  END AS long" +
           ") ) " +
           "FROM Loan l JOIN l.book b JOIN l.member u " +
           "WHERE l.member.userId = :memberId ORDER BY l.loanDate DESC")
    List<LoanDetailsDto> findLoanDetailsByMemberId(@Param("memberId") Integer memberId);

    @Query("SELECT l.book.id as bookId, COUNT(l.book.id) as loanCount FROM Loan l GROUP BY l.book.id ORDER BY loanCount DESC")
    List<Map<String, Object>> findMostLoanedBooks(Pageable pageable);

    @Query("SELECT l.member.userId as memberId, COUNT(l.member.userId) as loanCount FROM Loan l GROUP BY l.member.userId ORDER BY loanCount DESC")
    List<Map<String, Object>> findTopBorrowers(Pageable pageable);

    @Query("SELECT new com.ibizabroker.lms.dto.FineDetailsDto(l.id, b.name, u.name, l.dueDate, l.returnDate, l.fineAmount) " +
           "FROM Loan l JOIN l.book b JOIN l.member u " +
           "WHERE l.fineStatus = 'UNPAID' ORDER BY l.returnDate DESC")
    List<FineDetailsDto> findUnpaidFineDetails();

    @Query("SELECT new com.ibizabroker.lms.dto.FineDetailsDto(l.id, b.name, u.name, l.dueDate, l.returnDate, l.fineAmount) " +
           "FROM Loan l JOIN l.book b JOIN l.member u " +
           "WHERE l.fineStatus = 'UNPAID' AND l.member.userId = :memberId ORDER BY l.returnDate DESC")
    List<FineDetailsDto> findUnpaidFineDetailsByMemberId(@Param("memberId") Integer memberId);
    
    List<Loan> findByStatusAndDueDate(LoanStatus status, LocalDate dueDate);
    
    // === Query cho Chart.js: Thống kê mượn theo tháng ===
    @Query(value = "SELECT MONTH(l.loan_date) AS month, COUNT(l.id) AS count " +
                   "FROM loans l " +
                   "WHERE l.loan_date BETWEEN :start AND :end " +
                   "GROUP BY MONTH(l.loan_date) " +
                   "ORDER BY month ASC", nativeQuery = true)
    List<Map<String, Object>> findLoanCountsByMonth(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // === Query cho Chart.js: Top sách mượn nhiều ===
    @Query(value = "SELECT b.name AS bookName, COUNT(l.id) AS loanCount " +
                   "FROM loans l JOIN books b ON l.book_id = b.id " +
                   "WHERE l.loan_date BETWEEN :start AND :end " +
                   "GROUP BY b.name " +
                   "ORDER BY loanCount DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<Map<String, Object>> findMostLoanedBooksInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // === Query cho Chart.js: Trạng thái đơn mượn ===
    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> countLoansByStatus();

       // Tổng tất cả tiền phạt đã phát sinh
       @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM Loan l")
       BigDecimal getTotalFines();

       // Tổng tiền phạt chưa thanh toán
       @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM Loan l WHERE l.fineStatus = 'UNPAID'")
       BigDecimal getTotalUnpaidFines();

    // === Report Export Queries ===
    
    @Query("SELECT new com.ibizabroker.lms.dto.LoanDetailsDto(" +
           "l.id, b.name, u.name, l.loanDate, l.dueDate, l.returnDate, l.status, " +
           "l.fineAmount, " +
           "CAST(" +
           "  CASE " +
           "    WHEN l.returnDate IS NOT NULL AND l.returnDate > l.dueDate " +
           "      THEN (l.returnDate - l.dueDate) " +
           "    WHEN l.status = com.ibizabroker.lms.entity.LoanStatus.OVERDUE " +
           "      THEN (CURRENT_DATE - l.dueDate) " +
           "    ELSE 0 " +
           "  END AS long" +
           ") ) " +
           "FROM Loan l JOIN l.book b JOIN l.member u " +
           "WHERE l.loanDate BETWEEN :startDate AND :endDate " +
           "ORDER BY l.loanDate DESC")
    List<LoanDetailsDto> findLoanDetailsForReport(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.loanDate BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = 'RETURNED' AND l.returnDate BETWEEN :startDate AND :endDate")
    long countReturnedByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = 'OVERDUE' AND l.loanDate BETWEEN :startDate AND :endDate")
    long countOverdueByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(l.fineAmount), 0) FROM Loan l WHERE l.returnDate BETWEEN :startDate AND :endDate")
    BigDecimal sumFinesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}