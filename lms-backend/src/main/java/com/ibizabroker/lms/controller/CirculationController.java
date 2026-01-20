package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.BatchLoanRequest;
import com.ibizabroker.lms.dto.BatchLoanResponse;
import com.ibizabroker.lms.dto.FineDetailsDto;
import com.ibizabroker.lms.dto.LoanDetailsDto;
import com.ibizabroker.lms.dto.LoanRequest;
import com.ibizabroker.lms.dto.RenewRequest;
import com.ibizabroker.lms.entity.RenewalRequest;
import com.ibizabroker.lms.dto.ReservationRequest;
import com.ibizabroker.lms.dto.ReturnLoanResponseDto;
import com.ibizabroker.lms.entity.Loan;
import com.ibizabroker.lms.entity.Reservation;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.CirculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/circulation") // <--- THÊM /api VÀO ĐÂY
@CrossOrigin("http://localhost:4200/")
@RequiredArgsConstructor
public class CirculationController {

    private final CirculationService service;
    // Bổ sung các repository cần thiết
    private final LoanRepository loanRepository;
    private final UsersRepository usersRepository;

    // ---------- LOAN CRUD ----------
    @PostMapping("/loans")
    public ResponseEntity<Loan> loan(@RequestBody LoanRequest req) {
        return ResponseEntity.ok(service.loanBook(req));
    }

    /**
     * Batch create loans - Admin Scanner Loan Mode
     * Creates multiple loans for a single user in one transaction
     */
    @PostMapping("/loans/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchLoanResponse> batchLoan(@RequestBody BatchLoanRequest req) {
        BatchLoanResponse response = service.batchCreateLoans(req);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/loans/{id}/return")
    public ResponseEntity<ReturnLoanResponseDto> returnLoan(@PathVariable Integer id) {
        Loan loan = service.returnBook(id);

        Long overdueDays = 0L;
        if (loan.getReturnDate() != null && loan.getDueDate() != null && loan.getReturnDate().isAfter(loan.getDueDate())) {
            overdueDays = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), loan.getReturnDate());
        }

        ReturnLoanResponseDto dto = new ReturnLoanResponseDto(
                loan.getId(),
                loan.getBookId(),
                loan.getMemberId(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getStatus(),
                loan.getFineAmount(),
                overdueDays
        );

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/loans/renew")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RenewalRequest> renew(@RequestBody RenewRequest req, @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found from token"));
        RenewalRequest created = service.requestRenewal(req, currentUser.getUserId());
        return ResponseEntity.accepted().body(created);
    }

    @GetMapping("/loans")
    public ResponseEntity<List<Loan>> listLoans(
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer bookId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        List<Loan> out = service.findLoans(memberId, bookId, activeOnly);
        return ResponseEntity.ok(out);
    }
    
    // === TỐI ƯU HÓA: ENDPOINT MỚI CHO "MY ACCOUNT" ===
    @GetMapping("/my-loans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LoanDetailsDto>> getMyLoanHistory(@AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found from token"));
        
        List<LoanDetailsDto> loans = loanRepository.findLoanDetailsByMemberId(currentUser.getUserId());
        return ResponseEntity.ok(loans);
    }

    // ---------- RESERVATION ----------
    @PostMapping("/reservations")
    public ResponseEntity<Reservation> reserve(@RequestBody ReservationRequest req) {
        return ResponseEntity.ok(service.placeReservation(req));
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Integer id) {
        service.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> listReservations(@RequestParam Integer memberId) {
        List<Reservation> reservations = service.findReservations(memberId);
        return ResponseEntity.ok(reservations);
    }

    // ---------- FINES (API an toàn cho người dùng) ----------
    @GetMapping("/my-fines")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FineDetailsDto>> getMyFines(@AuthenticationPrincipal UserDetails userDetails) {
        // Lấy thông tin người dùng từ token, không tin tưởng ID từ client
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found for token"));

        // Lấy danh sách phí phạt dựa trên ID người dùng đã được xác thực
        List<FineDetailsDto> fines = loanRepository.findUnpaidFineDetailsByMemberId(currentUser.getUserId());
        return ResponseEntity.ok(fines);
    }

    // ---------- MY RENEWAL REQUESTS ----------
    @GetMapping("/my-renewals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RenewalRequest>> getMyRenewals(@AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found for token"));
        return ResponseEntity.ok(service.findRenewalsByMember(currentUser.getUserId()));
    }
}