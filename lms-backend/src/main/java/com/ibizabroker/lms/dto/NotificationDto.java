package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ibizabroker.lms.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 📢 WebSocket Notification DTO
 * 
 * Dùng để gửi real-time notifications từ Server → Client qua WebSocket.
 * 
 * 📌 Use Cases:
 * - Loan approval: "Yêu cầu mượn sách 'Clean Code' đã được duyệt"
 * - Fine payment: "Khoản phạt 20,000 VNĐ đã được thanh toán"
 * - Book return reminder: "Sách 'Java 21' sắp đến hạn trả (còn 2 ngày)"
 * - Renewal status: "Yêu cầu gia hạn đã được từ chối"
 * - Admin broadcast: "Thư viện đóng cửa ngày 30/1 vì bảo trì hệ thống"
 * 
 * 📌 Notification Types:
 * - INFO: Thông báo thông thường (màu xanh)
 * - SUCCESS: Thành công (màu xanh lá, icon ✓)
 * - WARNING: Cảnh báo (màu vàng, icon ⚠)
 * - ERROR: Lỗi (màu đỏ, icon ✗)
 * - URGENT: Khẩn cấp (màu đỏ, nổi bật, có âm thanh)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto {
    
    /**
     * ID notification (dùng để mark as read)
     */
    private Long notificationId;
    
    /**
     * Loại thông báo
     */
    @Builder.Default
    private NotificationType type = NotificationType.INFO;
    
    /**
     * Tiêu đề (hiển thị đậm)
     */
    private String title;
    
    /**
     * Nội dung chi tiết
     */
    private String message;
    
    /**
     * Link redirect khi user click vào notification
     * Ví dụ: "/my-account/loans", "/books/123"
     */
    private String actionUrl;
    
    /**
     * Text của action button (mặc định: "Xem chi tiết")
     */
    private String actionText;
    
    /**
     * Timestamp tạo notification (UTC)
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Đã đọc chưa (Frontend set true khi user click)
     */
    @Builder.Default
    private Boolean isRead = false;
    
    /**
     * Priority level (1=thấp, 5=cao)
     */
    @Builder.Default
    private Integer priority = 3;
    
    // ============= BUILDER HELPERS =============
    
    /**
     * Tạo notification thành công
     */
    public static NotificationDto success(String title, String message) {
        return NotificationDto.builder()
                .type(NotificationType.SUCCESS)
                .title(title)
                .message(message)
                .build();
    }
    
    /**
     * Tạo notification cảnh báo
     */
    public static NotificationDto warning(String title, String message) {
        return NotificationDto.builder()
                .type(NotificationType.WARNING)
                .title(title)
                .message(message)
                .priority(4)
                .build();
    }
    
    /**
     * Tạo notification lỗi
     */
    public static NotificationDto error(String title, String message) {
        return NotificationDto.builder()
                .type(NotificationType.ERROR)
                .title(title)
                .message(message)
                .priority(4)
                .build();
    }
    
    /**
     * Tạo notification khẩn cấp
     */
    public static NotificationDto urgent(String title, String message) {
        return NotificationDto.builder()
                .type(NotificationType.URGENT)
                .title(title)
                .message(message)
                .priority(5)
                .build();
    }
    
    /**
     * Tạo notification với action button
     */
    public static NotificationDto withAction(String title, String message, String actionUrl, String actionText) {
        return NotificationDto.builder()
                .type(NotificationType.INFO)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .actionText(actionText)
                .build();
    }
}
