package com.ibizabroker.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryScanRequest {
    @NotBlank
    private String code;
    private String shelfCode;
}
