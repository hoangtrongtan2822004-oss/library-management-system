package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.DashboardDetailsDto;
import com.ibizabroker.lms.dto.DashboardStatsDto;
import com.ibizabroker.lms.entity.LoanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AdminDashboardController {

    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;
    private final LoanRepository loanRepository;

    @GetMapping("/details") // THAY ĐỔI ENDPOINT
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDetailsDto> getDashboardDetails() {
        // Lấy thống kê cơ bản
        long totalBooks = booksRepository.count();
        long totalUsers = usersRepository.count();
        long activeLoans = loanRepository.countByStatus(LoanStatus.ACTIVE);
        long overdueLoansCount = loanRepository.countByStatus(LoanStatus.OVERDUE);
        DashboardStatsDto stats = new DashboardStatsDto(totalBooks, totalUsers, activeLoans, overdueLoansCount, null, null);  // ✅ Add BigDecimal totalFines, totalUnpaidFines (null for now)

        // Thêm thống kê tiền phạt
        stats.setTotalFines(loanRepository.getTotalFines());
        stats.setTotalUnpaidFines(loanRepository.getTotalUnpaidFines());

        // Tạo đối tượng chi tiết
        DashboardDetailsDto details = new DashboardDetailsDto();
        details.setStats(stats);
        details.setMostLoanedBooks(loanRepository.findMostLoanedBooks(PageRequest.of(0, 5)));
        details.setTopBorrowers(loanRepository.findTopBorrowers(PageRequest.of(0, 5)));
        details.setRecentActivities(loanRepository.findTop5ByOrderByLoanDateDesc());
        details.setOverdueLoans(loanRepository.findByStatus(LoanStatus.OVERDUE));

        return ResponseEntity.ok(details);
    }
}
