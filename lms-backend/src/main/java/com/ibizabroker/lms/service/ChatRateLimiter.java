package com.ibizabroker.lms.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatRateLimiter {

    // Dùng RAM thay vì Redis để đếm số lần chat (Tạm thời)
    private final Map<String, Long> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    public boolean isAllowed(String userId) {
        // Phiên bản đơn giản: Luôn cho phép chat (để demo cho dễ)
        // Nếu bạn muốn chặn spam thật thì code logic Java ở đây
        return true; 
    }

    public void clear(String userId) {
        requestCounts.remove(userId);
    }
}