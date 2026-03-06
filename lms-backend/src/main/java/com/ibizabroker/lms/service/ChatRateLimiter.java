package com.ibizabroker.lms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding-window rate limiter for chatbot endpoints.
 * OWASP A04 — Insecure Design: enforces per-user request quotas.
 *
 * Limits (in-memory, resets on restart):
 *   - 20 requests per 60 seconds  (sustained window)
 *   - 5  requests per 5  seconds  (burst window)
 */
@Service
public class ChatRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(ChatRateLimiter.class);

    static final int  MAX_REQUESTS_PER_WINDOW = 20;
    static final long WINDOW_MILLIS           = 60_000L;   // 60 s
    static final int  MAX_BURST_REQUESTS      = 5;
    static final long BURST_WINDOW_MILLIS     = 5_000L;    // 5 s

    /** Per-key deque of request timestamps (milliseconds). */
    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();
    private final AtomicLong callCounter = new AtomicLong();

    /**
     * Returns {@code true} if the request is allowed under the rate limits.
     * Thread-safe; uses synchronized block per key to avoid race conditions.
     */
    public boolean allow(String key) {
        long now = System.currentTimeMillis();

        // Periodic housekeeping every 500 calls to prevent unbounded map growth
        if (callCounter.incrementAndGet() % 500 == 0) {
            cleanupStaleEntries(now);
        }

        Deque<Long> ts = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (ts) {
            // Drop entries older than the sustained window
            while (!ts.isEmpty() && ts.peekFirst() < now - WINDOW_MILLIS) {
                ts.pollFirst();
            }

            // Check sustained limit (20 / 60 s)
            if (ts.size() >= MAX_REQUESTS_PER_WINDOW) {
                logger.warn("[RateLimit] Sustained limit exceeded — key={} count={}", key, ts.size());
                return false;
            }

            // Check burst limit (5 / 5 s)
            long burstCutoff = now - BURST_WINDOW_MILLIS;
            long burstCount = ts.stream().filter(t -> t >= burstCutoff).count();
            if (burstCount >= MAX_BURST_REQUESTS) {
                logger.warn("[RateLimit] Burst limit exceeded — key={} burstCount={}", key, burstCount);
                return false;
            }

            ts.addLast(now);
        }
        return true;
    }

    /**
     * Returns how many requests remain in the current 60-second window.
     */
    public int getRemainingRequests(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> ts = requestLog.get(key);
        if (ts == null) return MAX_REQUESTS_PER_WINDOW;
        synchronized (ts) {
            long used = ts.stream().filter(t -> t >= now - WINDOW_MILLIS).count();
            return (int) Math.max(0, MAX_REQUESTS_PER_WINDOW - used);
        }
    }

    /**
     * Returns seconds until the oldest request in the window expires (for Retry-After header).
     */
    public int getRetryAfterSeconds(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> ts = requestLog.get(key);
        if (ts == null || ts.isEmpty()) return 0;
        synchronized (ts) {
            Long oldest = ts.peekFirst();
            if (oldest == null) return 0;
            return (int) Math.max(1, (oldest + WINDOW_MILLIS - now) / 1000 + 1);
        }
    }

    /** Clears all recorded timestamps for a key (testing / admin reset). */
    public void clear(String key) {
        requestLog.remove(key);
    }

    private void cleanupStaleEntries(long now) {
        requestLog.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty()
                    || e.getValue().peekLast() < now - WINDOW_MILLIS;
            }
        });
    }
}