package com.ibizabroker.lms.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * OWASP security service for Chatbot endpoints.
 *
 * Covers:
 *   A03 — Injection:          prompt injection detection + input sanitization
 *   A04 — Insecure Design:    burst anomaly detection (complementary to ChatRateLimiter)
 *   A09 — Logging/Monitoring: structured security event logging (MDC-based, ELK/SIEM compatible)
 */
@Service
public class ChatSecurityService {

    /**
     * Dedicated security logger — can be routed to a separate appender/file
     * in logback.xml for SIEM ingestion.
     */
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");

    // ────────────────────────────────────────────────────
    // OWASP A03 — Prompt Injection Patterns
    // ────────────────────────────────────────────────────

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        // English: instruction override
        Pattern.compile("ignore\\s+(all\\s+)?previous\\s+instructions?",        Pattern.CASE_INSENSITIVE),
        Pattern.compile("forget\\s+(all\\s+)?(previous\\s+)?instructions?",     Pattern.CASE_INSENSITIVE),
        Pattern.compile("disregard\\s+(all\\s+)?(previous\\s+)?instructions?",  Pattern.CASE_INSENSITIVE),
        Pattern.compile("override\\s+your\\s+(previous\\s+)?instructions?",     Pattern.CASE_INSENSITIVE),
        Pattern.compile("do\\s+not\\s+follow\\s+(your\\s+)?instructions?",      Pattern.CASE_INSENSITIVE),
        // English: persona hijacking
        Pattern.compile("act\\s+as\\s+(if\\s+you\\s+are|a\\b|an\\b)",          Pattern.CASE_INSENSITIVE),
        Pattern.compile("pretend\\s+(you\\s+are|to\\s+be)\\b",                  Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+are\\s+now\\b",                                  Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+must\\s+now\\b",                                 Pattern.CASE_INSENSITIVE),
        Pattern.compile("roleplay\\s+as\\b",                                     Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjailbreak\\b",                                       Pattern.CASE_INSENSITIVE),
        Pattern.compile("new\\s+(role|persona|system\\s+prompt|instructions?)",  Pattern.CASE_INSENSITIVE),
        // English: system prompt extraction
        Pattern.compile("(print|reveal|show|output|repeat)\\s+(your|the)\\s+(system\\s+)?prompt", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(print|reveal|show|output|repeat)\\s+(your|the)\\s+instructions?",       Pattern.CASE_INSENSITIVE),
        Pattern.compile("what\\s+are\\s+your\\s+instructions?",                  Pattern.CASE_INSENSITIVE),
        // System-level token injection (LLM special tokens)
        Pattern.compile("<\\|system\\|>|<\\|im_start\\|>|<\\|endoftext\\|>",    Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[INST\\]|</s>|<s>|###\\s*System",                    Pattern.CASE_INSENSITIVE),
        Pattern.compile("```\\s*system",                                         Pattern.CASE_INSENSITIVE),
        // Vietnamese patterns
        Pattern.compile("bỏ\\s+qua\\s+hướng\\s+dẫn",                           Pattern.CASE_INSENSITIVE),
        Pattern.compile("quên\\s+(đi\\s+)?vai\\s+trò",                          Pattern.CASE_INSENSITIVE),
        Pattern.compile("bây\\s+giờ\\s+hãy\\s+là",                              Pattern.CASE_INSENSITIVE)
    );

    // Strip dangerous control characters (keep \n=0x0A, \t=0x09, \r=0x0D)
    private static final Pattern CONTROL_CHARS =
        Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    // ────────────────────────────────────────────────────
    // OWASP A09 — Anomaly Detection State
    // ────────────────────────────────────────────────────

    private static final long ANOMALY_WINDOW_MILLIS = 5 * 60_000L; // 5 minutes
    private static final int  ANOMALY_THRESHOLD     = 50;           // requests per window

    private final ConcurrentHashMap<String, Deque<Long>> anomalyLog = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────

    /**
     * OWASP A03: Sanitize a user prompt before processing.
     * <ul>
     *   <li>Strips null bytes and non-printable control characters</li>
     *   <li>Collapses excessive blank lines / whitespace runs</li>
     *   <li>Hard-truncates at 2 000 characters (after cleaning)</li>
     * </ul>
     */
    public String sanitizePrompt(String prompt) {
        if (prompt == null) return "";
        String clean = CONTROL_CHARS.matcher(prompt).replaceAll("");
        // Collapse 4+ consecutive newlines → 3
        clean = clean.replaceAll("\\n{4,}", "\n\n\n");
        // Collapse 3+ consecutive spaces/tabs → 2
        clean = clean.replaceAll("[ \\t]{3,}", "  ");
        return clean.length() > 2000 ? clean.substring(0, 2000) : clean;
    }

    /**
     * OWASP A03: Detect prompt injection attempts.
     *
     * @return {@code true} if a known injection pattern is found.
     */
    public boolean detectPromptInjection(String prompt) {
        if (prompt == null || prompt.isBlank()) return false;
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(prompt).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * OWASP A09: Log a structured security event.
     * MDC fields are set for downstream log aggregators (ELK, Splunk, etc.).
     *
     * @param eventType  e.g. "PROMPT_INJECTION_ATTEMPT", "RATE_LIMIT_HIT", "ANOMALY_BURST"
     * @param userId     caller identity (key used for rate-limiting)
     * @param ip         client IP address
     * @param detail     short human-readable context
     */
    public void logSecurityEvent(String eventType, String userId, String ip, String detail) {
        MDC.put("securityEvent", eventType);
        MDC.put("userId",        userId != null ? userId : "unknown");
        MDC.put("clientIp",      ip     != null ? ip     : "unknown");
        try {
            securityLog.warn("[SECURITY] {} | user={} | ip={} | detail={}",
                    eventType, userId, ip, sanitizeForLog(detail));
        } finally {
            MDC.remove("securityEvent");
            MDC.remove("userId");
            MDC.remove("clientIp");
        }
    }

    /**
     * OWASP A09: Detect anomalous request volume (>50 requests in 5 minutes).
     * Logs the event but does NOT block — caller decides how to handle it.
     *
     * @return {@code true} if the threshold has been exceeded.
     */
    public boolean detectAnomaly(String key, String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> times = anomalyLog.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < now - ANOMALY_WINDOW_MILLIS) {
                times.pollFirst();
            }
            times.addLast(now);
            if (times.size() > ANOMALY_THRESHOLD) {
                logSecurityEvent("ANOMALY_BURST", key, ip,
                    "Exceeded " + ANOMALY_THRESHOLD + " requests in 5 min (count=" + times.size() + ")");
                return true;
            }
        }
        return false;
    }

    /**
     * OWASP A07: Extract the real client IP, honouring reverse-proxy headers.
     * Accepts only strings that look like IPv4/IPv6 addresses to prevent
     * header injection from influencing log output.
     */
    public String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String firstIp = forwarded.split(",")[0].trim();
            // Allow only characters valid in IPv4/IPv6 addresses
            if (firstIp.matches("[\\d.:a-fA-F]{2,45}")) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }

    // ────────────────────────────────────────────────────
    // Internal helpers
    // ────────────────────────────────────────────────────

    /**
     * Prevents log injection: strip newlines and truncate log payloads.
     */
    private static String sanitizeForLog(String value) {
        if (value == null) return "";
        String safe = value.replace("\n", "\\n").replace("\r", "\\r");
        return safe.length() > 200 ? safe.substring(0, 200) + "…" : safe;
    }
}
