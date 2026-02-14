package com.ibizabroker.lms.dto;

import com.ibizabroker.lms.entity.AuditLogEntry;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogResponseDto {
    private Long id;
    private String actor;
    private String actorRoles;
    private String action;
    private String resource;
    private String targetId;
    private String httpMethod;
    private String path;
    private String ip;
    private String userAgent;
    private String status;
    private String errorMessage;
    private String requestPayload;
    private String responsePayload;
    private LocalDateTime createdAt;

    public static AuditLogResponseDto fromEntity(AuditLogEntry entry) {
        if (entry == null) return null;
        return AuditLogResponseDto.builder()
            .id(entry.getId())
            .actor(entry.getActor())
            .actorRoles(entry.getActorRoles())
            .action(entry.getAction())
            .resource(entry.getResource())
            .targetId(entry.getTargetId())
            .httpMethod(entry.getHttpMethod())
            .path(entry.getPath())
            .ip(entry.getIp())
            .userAgent(entry.getUserAgent())
            .status(entry.getStatus())
            .errorMessage(entry.getErrorMessage())
            .requestPayload(entry.getRequestPayload())
            .responsePayload(entry.getResponsePayload())
            .createdAt(entry.getCreatedAt())
            .build();
    }
}
