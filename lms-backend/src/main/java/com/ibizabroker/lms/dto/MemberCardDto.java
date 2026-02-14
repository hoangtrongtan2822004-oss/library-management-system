package com.ibizabroker.lms.dto;

import com.ibizabroker.lms.entity.MemberCard;
import com.ibizabroker.lms.entity.MemberCardStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberCardDto {
    private Long id;
    private String cardNumber;
    private String barcodeType;
    private MemberCardStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
    private String metadata;
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String studentClass;

    public static MemberCardDto fromEntity(MemberCard card) {
        if (card == null) return null;
        var user = card.getUser();
        return MemberCardDto.builder()
            .id(card.getId())
            .cardNumber(card.getCardNumber())
            .barcodeType(card.getBarcodeType() != null ? card.getBarcodeType().name() : null)
            .status(card.getStatus())
            .issuedAt(card.getIssuedAt())
            .expiredAt(card.getExpiredAt())
            .metadata(card.getMetadata())
            .userId(user != null ? user.getUserId() : null)
            .username(user != null ? user.getUsername() : null)
            .fullName(user != null ? user.getName() : null)
            .email(user != null ? user.getEmail() : null)
            .studentClass(user != null ? user.getStudentClass() : null)
            .build();
    }
}
