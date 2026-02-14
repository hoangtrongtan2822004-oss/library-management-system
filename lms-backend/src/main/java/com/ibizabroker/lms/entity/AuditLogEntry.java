package com.ibizabroker.lms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String actor;

    @Column(name = "actor_roles", length = 200)
    private String actorRoles;

    @Column(length = 120)
    private String action;

    @Column(length = 80)
    private String resource;

    @Column(name = "target_id", length = 80)
    private String targetId;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(length = 200)
    private String path;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(length = 20)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Lob
    @Column(name = "request_payload")
    private String requestPayload;

    @Lob
    @Column(name = "response_payload")
    private String responsePayload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
