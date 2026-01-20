package com.ibizabroker.lms.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatRateLimiter {

    // Dùng RAM thay vì Redis
    private final Map<String, Long> requestCounts = new ConcurrentHashMap<>();

    // ✅ Đã đổi tên hàm thành 'allow' để khớp với Controller
    public boolean allow(String userId) {
        // Luôn cho phép chat (Bỏ qua giới hạn để chạy được đã)
        return true; 
    }

    public void clear(String userId) {
        requestCounts.remove(userId);
    }
}