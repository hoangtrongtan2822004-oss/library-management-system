package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/admin/fines")
public class FinesReportController {

    private final LoanRepository loanRepository;

    @GetMapping("/daily-summary")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDailySummary(@RequestParam(required = false) String date) {
        LocalDate target;
        try {
            target = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        } catch (Exception ex) {
            target = LocalDate.now();
        }

        BigDecimal totalAmount = loanRepository.sumFinesByDateRange(target, target);
        long totalCount = loanRepository.countReturnedByDateRange(target, target);

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        resp.put("totalCount", totalCount);
        resp.put("byMethod", List.of());

        return ResponseEntity.ok(resp);
    }
}
