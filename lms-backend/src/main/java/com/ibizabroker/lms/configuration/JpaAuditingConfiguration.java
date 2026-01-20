package com.ibizabroker.lms.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * ⚙️ JPA Auditing Configuration
 * 
 * Tự động điền các trường audit trong BaseEntity:
 * - createdAt: Thời gian tạo (tự động)
 * - updatedAt: Thời gian cập nhật (tự động)
 * - createdBy: User tạo (từ Spring Security)
 * - updatedBy: User cập nhật (từ Spring Security)
 * 
 * 🎯 How it works:
 * 1. Entity extends BaseEntity
 * 2. @CreatedDate, @LastModifiedDate → Spring tự động set timestamp
 * 3. @CreatedBy, @LastModifiedBy → Gọi auditorProvider() để lấy username
 * 4. auditorProvider() lấy username từ SecurityContextHolder
 * 
 * 📌 Fallback:
 * - Nếu không có Authentication (e.g., data seeding) → dùng "system"
 * - Anonymous users → dùng "anonymous"
 * 
 * @author Library Management System
 * @since Phase 6: Entity Layer Upgrade
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {

    /**
     * 👤 AuditorAware Bean
     * 
     * Spring JPA gọi method này để lấy current user cho @CreatedBy/@LastModifiedBy
     * 
     * Logic:
     * 1. Lấy SecurityContext từ Spring Security
     * 2. Lấy Authentication object
     * 3. Check isAuthenticated() để loại trừ anonymous
     * 4. Lấy username (email hoặc username tùy thuộc UserDetails implementation)
     * 5. Fallback: "system" nếu không có authentication
     * 
     * @return Optional<String> chứa username hoặc "system"
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .filter(name -> !"anonymousUser".equals(name))
            .or(() -> Optional.of("system"));
    }
}
