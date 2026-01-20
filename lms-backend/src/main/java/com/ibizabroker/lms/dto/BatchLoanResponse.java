package com.ibizabroker.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchLoanResponse {
    private List<LoanResult> results;
    private int successCount;
    private int failureCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanResult {
        private Integer bookId;
        private Integer loanId; // null if failed
        private boolean success;
        private String errorMessage; // null if success
    }
}
