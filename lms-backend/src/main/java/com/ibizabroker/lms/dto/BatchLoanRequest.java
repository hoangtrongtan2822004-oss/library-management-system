package com.ibizabroker.lms.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchLoanRequest {
    private Integer userId;
    private List<Integer> bookIds;
    private Integer loanDays; // Optional: custom loan duration
}
