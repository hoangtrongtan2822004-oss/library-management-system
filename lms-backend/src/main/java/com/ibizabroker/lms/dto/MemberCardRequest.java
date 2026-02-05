package com.ibizabroker.lms.dto;

import com.ibizabroker.lms.entity.MemberCard;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MemberCardRequest(
        @NotNull Long userId,
        @NotNull MemberCard.BarcodeType barcodeType,
        @FutureOrPresent(message = "Ngày hết hạn phải từ hiện tại trở đi") LocalDateTime expiredAt,
        @Size(max = 2000) String metadata
) {}
