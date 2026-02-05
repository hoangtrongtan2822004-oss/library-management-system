package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dto.FineDetailsDto;
import com.ibizabroker.lms.entity.FineStatus;
import com.ibizabroker.lms.entity.Loan;
import com.ibizabroker.lms.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/admin/fines") // <-- THAY ĐỔI
public class FineController {

    private final LoanRepository loanRepository;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<FineDetailsDto>> getUnpaidFines() {
        return ResponseEntity.ok(loanRepository.findUnpaidFineDetails());
    }

    @GetMapping("/paid")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<FineDetailsDto>> getPaidFines(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (startDate != null && endDate != null) {
            java.time.LocalDate s = java.time.LocalDate.parse(startDate);
            java.time.LocalDate e = java.time.LocalDate.parse(endDate);
            return ResponseEntity.ok(loanRepository.findPaidFineDetailsBetween(s, e));
        }

        return ResponseEntity.ok(loanRepository.findPaidFineDetails());
    }

    @PostMapping("/{loanId}/pay")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> markFineAsPaid(@PathVariable Integer loanId) {
        @SuppressWarnings("null")
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found"));
        loan.setFineStatus(FineStatus.PAID);  // ✅ Use enum
        loanRepository.save(loan);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/daily-summary")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDailySummary(@RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        BigDecimal total = loanRepository.sumPaidFinesByDate(d);
        long count = loanRepository.countPaidFinesByDate(d);

        Map<String, Object> resp = Map.of(
                "totalAmount", total,
                "totalCount", count,
                "byMethod", Collections.emptyList()
        );

        return ResponseEntity.ok(resp);
    }
    
    // Phương thức getMyFines đã được xóa và sẽ chuyển sang CirculationController
}