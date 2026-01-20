package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 🏛️ Base Entity - JPA Auditing
 * 
 * Entity cha chứa các trường audit tự động:
 * - createdAt: Thời gian tạo (tự động)
 * - updatedAt: Thời gian cập nhật cuối (tự động)
 * - createdBy: Người tạo (tự động từ SecurityContext)
 * - updatedBy: Người cập nhật cuối (tự động từ SecurityContext)
 * 
 * 📌 Usage:
 * ```java
 * @Entity
 * public class Books extends BaseEntity {
 *     // Không cần khai báo createdAt, updatedAt nữa!
 * }
 * ```
 * 
 * ⚠️ Requires: Enable JPA Auditing trong @Configuration:
 * ```java
 * @EnableJpaAuditing
 * public class JpaAuditingConfiguration {
 *     @Bean public AuditorAware<String> auditorProvider() {...}
 * }
 * ```
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 🕒 Thời gian tạo record
     * Auto-filled by JPA Auditing
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 🕒 Thời gian cập nhật cuối
     * Auto-updated by JPA Auditing
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 👤 Người tạo record (username)
     * Auto-filled from SecurityContext
     */
    @CreatedBy
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    /**
     * 👤 Người cập nhật cuối (username)
     * Auto-updated from SecurityContext
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
