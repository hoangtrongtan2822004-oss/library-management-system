package com.ibizabroker.lms.configuration;

import com.ibizabroker.lms.entity.AuditLogEntry;
import com.ibizabroker.lms.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@RequiredArgsConstructor
public class AdminAuditLogFilter extends OncePerRequestFilter {

    private static final int MAX_PAYLOAD_LENGTH = 4000;
    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            try {
                logIfAdminAction(requestWrapper, responseWrapper);
            } catch (Exception ignored) {
                // Avoid breaking request flow due to audit logging
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logIfAdminAction(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/admin")) {
            return;
        }

        // Only log state-changing operations; skip read-only requests
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return;
        }

        boolean isAdmin = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));
        if (!isAdmin) {
            return;
        }

        AuditLogEntry entry = new AuditLogEntry();
        entry.setActor(auth.getName());
        entry.setActorRoles(auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(",")));
        entry.setHttpMethod(request.getMethod());
        entry.setPath(path);
        entry.setIp(request.getRemoteAddr());
        entry.setUserAgent(request.getHeader("User-Agent"));

        int status = response.getStatus();
        entry.setStatus(status < 400 ? "SUCCESS" : "FAIL");
        entry.setAction(deriveAction(request.getMethod(), path));
        entry.setResource(deriveResource(path));
        entry.setTargetId(deriveTargetId(path));

        String requestPayload = extractRequestPayload(request);
        entry.setRequestPayload(requestPayload);

        String responsePayload = extractResponsePayload(response);
        entry.setResponsePayload(responsePayload);

        if (status >= 400 && responsePayload != null && !responsePayload.isBlank()) {
            entry.setErrorMessage(truncatePayload(responsePayload));
        }

        auditLogService.save(entry);
    }

    private String extractRequestPayload(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains(MediaType.APPLICATION_JSON_VALUE)) {
            return truncatePayload(getCachedPayload(request.getContentAsByteArray(), request.getCharacterEncoding()));
        }
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            return truncatePayload(request.getQueryString());
        }
        return null;
    }

    private String extractResponsePayload(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        if (contentType == null) return null;
        if (contentType.toLowerCase(Locale.ROOT).contains(MediaType.APPLICATION_JSON_VALUE) ||
            contentType.toLowerCase(Locale.ROOT).contains(MediaType.TEXT_PLAIN_VALUE)) {
            return truncatePayload(getCachedPayload(response.getContentAsByteArray(), response.getCharacterEncoding()));
        }
        return null;
    }

    private String getCachedPayload(byte[] content, String encoding) {
        if (content == null || content.length == 0) return null;
        Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        return new String(content, charset).trim();
    }

    private String truncatePayload(String payload) {
        if (payload == null) return null;
        if (payload.length() <= MAX_PAYLOAD_LENGTH) return payload;
        return payload.substring(0, MAX_PAYLOAD_LENGTH) + "...";
    }

    private String deriveAction(String method, String path) {
        String resource = deriveResource(path);
        if (resource == null) return method;
        return method.toUpperCase(Locale.ROOT) + "_" + resource;
    }

    private String deriveResource(String path) {
        if (path == null) return null;
        String[] parts = path.split("/");
        if (parts.length < 4) return null;
        return parts[3].toUpperCase(Locale.ROOT);
    }

    private String deriveTargetId(String path) {
        if (path == null) return null;
        String[] parts = path.split("/");
        if (parts.length >= 5) {
            String candidate = parts[4];
            if (!candidate.isBlank()) return candidate;
        }
        return null;
    }
}
