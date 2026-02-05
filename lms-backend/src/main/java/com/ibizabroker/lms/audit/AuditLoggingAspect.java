package com.ibizabroker.lms.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.lms.entity.AuditLog;
import com.ibizabroker.lms.service.AuditLogService;
import com.ibizabroker.lms.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private static final int MAX_PAYLOAD_LENGTH = 2000;

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @AfterReturning(
            pointcut = "within(@" +
                    "org.springframework.web.bind.annotation.RestController" +
                    " *) && execution(* com.ibizabroker.lms.controller..*(..))",
            returning = "result")
    public void logAfterSuccess(JoinPoint joinPoint, Object result) {
        handleAudit(joinPoint, "SUCCESS", null, result);
    }

    @AfterThrowing(
            pointcut = "within(@" +
                    "org.springframework.web.bind.annotation.RestController" +
                    " *) && execution(* com.ibizabroker.lms.controller..*(..))",
            throwing = "ex")
    public void logAfterFailure(JoinPoint joinPoint, Throwable ex) {
        handleAudit(joinPoint, "FAILURE", ex.getMessage(), null);
    }

    private void handleAudit(JoinPoint joinPoint, String status, String errorMessage, Object result) {
        // Only log when current user is admin
        if (!SecurityUtils.hasRole("ADMIN")) {
            return;
        }

        HttpServletRequest request = currentRequest();
        String username = SecurityUtils.getCurrentUsername().orElse("anonymous");
        Set<String> roles = SecurityUtils.getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(granted -> granted.getAuthority())
                        .collect(Collectors.toCollection(HashSet::new)))
                .orElseGet(HashSet::new);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditLog logEntry = AuditLog.builder()
                .actor(username)
                .actorRoles(String.join(",", roles))
                .action(signature.getMethod().getName())
                .resource(signature.getDeclaringType().getSimpleName())
                .status(status)
                .errorMessage(truncate(errorMessage, 500))
                .httpMethod(request != null ? request.getMethod() : null)
                .path(request != null ? request.getRequestURI() : null)
                .ip(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .requestPayload(safePayload(joinPoint.getArgs()))
                .responsePayload(status.equals("SUCCESS") ? safeJson(result) : null)
                .build();

        auditLogService.save(logEntry);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String safePayload(Object[] args) {
        try {
            Object[] filtered = Arrays.stream(args)
                    .filter(arg -> arg != null)
                    .filter(arg -> !(arg instanceof HttpServletRequest))
                    .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                    .filter(arg -> !(arg instanceof org.springframework.validation.BindingResult))
                    .filter(arg -> !(arg instanceof org.springframework.web.multipart.MultipartFile))
                    .filter(arg -> !(arg instanceof org.springframework.security.core.Authentication))
                    .toArray();
            if (filtered.length == 0) {
                return null;
            }
            return truncate(objectMapper.writeValueAsString(filtered), MAX_PAYLOAD_LENGTH);
        } catch (Exception e) {
            log.debug("[Audit] Failed to serialize request payload: {}", e.getMessage());
            return null;
        }
    }

    private String safeJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(obj), MAX_PAYLOAD_LENGTH);
        } catch (Exception e) {
            log.debug("[Audit] Failed to serialize response payload: {}", e.getMessage());
            return truncate(String.valueOf(obj), MAX_PAYLOAD_LENGTH);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
