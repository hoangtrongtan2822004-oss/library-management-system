package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.AuditLogRepository;
import com.ibizabroker.lms.entity.AuditLogEntry;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @SuppressWarnings("null")
    public AuditLogEntry save(AuditLogEntry entry) {
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntry> findByFilters(
        String actor,
        String action,
        String status,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    ) {
        return auditLogRepository.findByFilters(actor, action, status, from, to, pageable);
    }
}
