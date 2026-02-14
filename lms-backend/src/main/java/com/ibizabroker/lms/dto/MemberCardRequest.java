package com.ibizabroker.lms.dto;

import com.ibizabroker.lms.entity.BarcodeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberCardRequest {
    @NotNull
    private Integer userId;

    @NotNull
    private BarcodeType barcodeType;

    private String expiredAt; // ISO-8601 string
    private String metadata;
}
