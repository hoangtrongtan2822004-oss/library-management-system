package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📊 Report Summary Response DTO (Admin)
 *
 * Dùng cho endpoint GET /api/admin/reports/summary
 *
 * This DTO now exposes typed lists for the main report sections instead of generic maps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportSummaryDto {
    private List<LoansByMonthEntry> loansByMonth;
    private List<MostLoanedBookEntry> mostLoanedBooks;
    private List<FinesByMonthEntry> finesByMonth;
}
