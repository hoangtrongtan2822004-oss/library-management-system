package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 📢 WebSocket Notification Service
 * 
 * Service để gửi real-time notifications qua WebSocket.
 * 
 * 📌 Cách sử dụng trong các Service khác:
 * 
 * ```java
 * @Autowired
 * private WebSocketNotificationService notificationService;
 * 
 * // Khi admin duyệt loan request
 * public void approveLoanRequest(Integer loanId) {
 *     // ... business logic ...
 *     
 *     notificationService.sendToUser(
 *         loan.getUser().getUsername(),
 *         NotificationDto.success(
 *             "Yêu cầu mượn sách đã được duyệt",
 *             "Bạn có thể đến thư viện để nhận sách '" + loan.getBook().getName() + "'"
 *         )
 *     );
 * }
 * 
 * // Khi có fine mới
 * public void createFine(Integer userId, BigDecimal amount) {
 *     // ... business logic ...
 *     
 *     notificationService.sendToUser(
 *         user.getUsername(),
 *         NotificationDto.warning(
 *             "Bạn có khoản phạt mới",
 *             "Số tiền: " + amount + " VNĐ. Vui lòng thanh toán trước ngày " + dueDate
 *         )
 *     );
 * }
 * 
 * // Broadcast thông báo quan trọng
 * public void announceLibraryClosure() {
 *     notificationService.broadcastToAll(
 *         NotificationDto.urgent(
 *             "Thư viện tạm đóng cửa",
 *             "Thư viện sẽ đóng cửa từ 30/1 - 5/2 để bảo trì hệ thống"
 *         )
 *     );
 * }
 * ```
 * 
 * 💰 Business Value:
 * - Instant notification: User biết ngay khi có update (không cần F5)
 * - Giảm server load: Không còn polling API mỗi 5 giây
 * - Better UX: Thông báo popup ngay trên màn hình
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Gửi notification đến 1 user cụ thể
     * 
     * @param username Username của user nhận notification
     * @param notification Notification data
     */
    @SuppressWarnings("null")
    public void sendToUser(String username, NotificationDto notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                notification
            );
            log.info("✅ Sent notification to user '{}': {}", username, notification.getTitle());
        } catch (Exception e) {
            log.error("❌ Failed to send notification to user '{}': {}", username, e.getMessage());
        }
    }
    
    /**
     * Broadcast notification đến tất cả users đang online
     * 
     * @param notification Notification data
     */
    @SuppressWarnings("null")
    public void broadcastToAll(NotificationDto notification) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/announcements",
                notification
            );
            log.info("📢 Broadcasted notification to all users: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("❌ Failed to broadcast notification: {}", e.getMessage());
        }
    }
    
    /**
     * Gửi notification đến nhóm users (theo role)
     * 
     * @param role Role của nhóm users (ADMIN, USER)
     * @param notification Notification data
     */
    @SuppressWarnings("null")
    public void sendToRole(String role, NotificationDto notification) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/role-" + role.toLowerCase(),
                notification
            );
            log.info("📢 Sent notification to role '{}': {}", role, notification.getTitle());
        } catch (Exception e) {
            log.error("❌ Failed to send notification to role '{}': {}", role, e.getMessage());
        }
    }
    
    /**
     * Gửi notification với action button
     */
    public void sendWithAction(String username, String title, String message, String actionUrl, String actionText) {
        NotificationDto notification = NotificationDto.withAction(title, message, actionUrl, actionText);
        sendToUser(username, notification);
    }
    
    /**
     * Gửi urgent notification (có âm thanh alert)
     */
    public void sendUrgent(String username, String title, String message) {
        NotificationDto notification = NotificationDto.urgent(title, message);
        sendToUser(username, notification);
    }
}
