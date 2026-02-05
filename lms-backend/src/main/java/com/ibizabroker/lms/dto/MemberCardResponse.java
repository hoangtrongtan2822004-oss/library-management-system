package com.ibizabroker.lms.dto;

import com.ibizabroker.lms.entity.MemberCard;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MemberCardResponse {
    private Long id;
    private String cardNumber;
    private MemberCard.BarcodeType barcodeType;
    private MemberCard.MemberCardStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
    private String metadata;

    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String studentClass;
}
