// lms-backend/src/main/java/com/ibizabroker/lms/controller/ReportController.java

package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dto.ReportSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final LoanRepository loanRepository;

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDto> getReportSummary(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        ReportSummaryDto summary = new ReportSummaryDto();

        // Map repository raw maps to typed DTOs
        var rawLoans = loanRepository.findLoanCountsByMonth(start, end);
        var loansList = new java.util.ArrayList<com.ibizabroker.lms.dto.LoansByMonthEntry>();
        for (Map<String, Object> m : rawLoans) {
            Object monthObj = m.get("month");
            Object countObj = m.get("count");
            String monthStr = monthObj == null ? "" : monthObj.toString();
            long count = 0L;
            if (countObj instanceof Number) count = ((Number) countObj).longValue();
            loansList.add(new com.ibizabroker.lms.dto.LoansByMonthEntry(monthStr, count));
        }

        var rawTop = loanRepository.findMostLoanedBooksInPeriod(start, end);
        var topList = new java.util.ArrayList<com.ibizabroker.lms.dto.MostLoanedBookEntry>();
        for (Map<String, Object> m : rawTop) {
            Object nameObj = m.get("bookName");
            Object countObj = m.get("loanCount");
            String name = nameObj == null ? "" : nameObj.toString();
            long cnt = 0L;
            if (countObj instanceof Number) cnt = ((Number) countObj).longValue();
            topList.add(new com.ibizabroker.lms.dto.MostLoanedBookEntry(null, name, cnt));
        }

        summary.setLoansByMonth(loansList);
        summary.setMostLoanedBooks(topList);
        // Populate finesByMonth via repository aggregation
        var rawFines = loanRepository.findFinesByMonth(start, end);
        var finesList = new java.util.ArrayList<com.ibizabroker.lms.dto.FinesByMonthEntry>();
        for (Map<String, Object> m : rawFines) {
            String month = m.get("month") == null ? "" : m.get("month").toString();
            long total = 0L;
            Object totalObj = m.get("totalFines");
            if (totalObj instanceof Number) total = ((Number) totalObj).longValue();
            finesList.add(new com.ibizabroker.lms.dto.FinesByMonthEntry(month, total));
        }

        summary.setFinesByMonth(finesList);

        return ResponseEntity.ok(summary);
    }
}