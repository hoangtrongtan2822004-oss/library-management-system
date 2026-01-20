package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.ChatMessageRepository;
import com.ibizabroker.lms.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// Đã xóa import Redis để fix lỗi build
// import org.springframework.data.redis.core.StringRedisTemplate;
// import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {
    
    private final ChatMessageRepository chatMessageRepository;
    // Đã xóa redisTemplate vì không dùng Redis nữa
    // private final StringRedisTemplate redisTemplate;

    // private static final long CACHE_EXPIRATION_MINUTES = 60;

    /**
     * Generate a new conversation ID
     */
    public String generateConversationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Save a chat message exchange
     */
    public ChatMessage saveMessage(String conversationId, Integer userId, String userMessage, String botResponse) {
        ChatMessage message = new ChatMessage(conversationId, userId, userMessage, botResponse);
        return chatMessageRepository.save(message);
    }

    /**
     * Save conversation history to Redis cache
     * (Đã vô hiệu hóa - No Op)
     */
    public void saveHistoryToCache(String conversationId, String history) {
        // Không làm gì cả vì không dùng Redis
    }

    /**
     * Get conversation history from Redis cache
     * (Đã vô hiệu hóa - Return null)
     */
    public String getHistoryFromCache(String conversationId) {
        // Trả về null để hệ thống tự load từ Database
        return null;
    }

    /**
     * Clear conversation history from Redis cache
     * (Đã vô hiệu hóa - No Op)
     */
    public void clearHistoryFromCache(String conversationId) {
        // Không làm gì cả
    }

    /**
     * Get conversation history
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversationHistory(String conversationId) {
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Get recent messages for context (up to 5 last messages)
     */
    /**
     * Xây dựng ngữ cảnh hội thoại (System Prompt + Lịch sử)
     */
    @Transactional(readOnly = true)
    public String buildConversationContext(String conversationId) {
        // 1. Định nghĩa "nhân cách" và quy định của trường
        String systemContext = """
            Bối cảnh: Bạn là Trợ lý ảo AI của Thư viện Trường THCS Phương Tú (Ứng Hòa, Hà Nội).
            Đối tượng phục vụ: Học sinh (lớp 6-9) và Giáo viên nhà trường.

            Nhiệm vụ chính:
            1) Giới thiệu sách phù hợp lứa tuổi (SGK, tham khảo, kỹ năng sống, văn học tuổi teen) DỰA TRÊN NGỮ CẢNH/CONTEXT ĐƯỢC CUNG CẤP.
            2) Giải đáp quy định thư viện thân thiện, lễ phép.

            Quy định thư viện:
            - Mở cửa: 7h30 - 16h30 (Thứ 2 - Thứ 6).
            - Mượn tối đa: 3 cuốn / lần.
            - Thời hạn mượn: 14 ngày.
            - Mất sách: Đền sách mới hoặc tiền tương đương giá bìa + 20% phí xử lý.

            QUY TẮC BẮT BUỘC (chống ảo giác):
            - Chỉ gợi ý các tựa sách/câu trả lời có trong CONTEXT/RAG cung cấp. KHÔNG dùng kiến thức chung hay phỏng đoán.
            - Nếu CONTEXT không chứa sách/phần liên quan, trả lời: "Hiện tại mình chưa tìm thấy sách phù hợp trong thư viện. Bạn có thể hỏi thủ thư để kiểm tra thêm nhé." (hoặc gợi ý một sách có thật trong context nếu có).
            - Nếu người dùng hỏi "tiểu thuyết giả tưởng/fantasy" mà CONTEXT không có tựa phù hợp, hãy đề xuất các tựa gần nhất theo thể loại có trong CONTEXT (ví dụ: truyện cổ tích, truyện thiếu nhi, truyện phiêu lưu) và nêu rõ đây là gợi ý thay thế.
            - Không hứa hẹn "có sẵn" khi không chắc chắn; ưu tiên hướng dẫn bạn đọc xuống thư viện kiểm tra.
            - Luôn xưng hô "mình" với học sinh; "em" nếu người hỏi là giáo viên.
            - Văn phong: trong sáng, khuyến khích đọc sách, tránh thuật ngữ nặng.
            """;

        // 2. Lấy lịch sử chat cũ (nếu có)
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessages(conversationId);
        
        if (recentMessages.isEmpty()) {
            return systemContext + "\n\nĐây là bắt đầu cuộc hội thoại.";
        }
        
        StringBuilder context = new StringBuilder(systemContext + "\n\nLịch sử chat gần đây:\n");
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            ChatMessage msg = recentMessages.get(i);
            context.append("User: ").append(msg.getUserMessage()).append("\n");
            context.append("Assistant: ").append(msg.getBotResponse()).append("\n");
        }
        
        return context.toString();
    }

    /**
     * Get all conversations for a user
     */
    @Transactional(readOnly = true)
    public List<String> getUserConversations(Integer userId) {
        return chatMessageRepository.findUserConversationIds(userId);
    }

    /**
     * Build augmented prompt with conversation context
     */
    public String buildContextAwarePrompt(String userQuery, String conversationId) {
        String conversationContext = buildConversationContext(conversationId);
        
        return conversationContext + "\n" +
                "Current user question: " + userQuery + "\n\n" +
                "Please provide a helpful response based on the conversation context and the user's current question.";
    }
}