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
    
    // Phương thức getMyFines đã được xóa và sẽ chuyển sang CirculationController
}