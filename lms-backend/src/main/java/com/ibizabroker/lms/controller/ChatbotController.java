package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ChatRequestDto;
import com.ibizabroker.lms.service.ConversationService;
import com.ibizabroker.lms.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*; // Sửa lại import
import org.springframework.util.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException; // ✅ ĐÃ SỬA
import org.springframework.web.client.HttpClientErrorException;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.entity.Users;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.lms.service.ChatRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/user/chat")
@PreAuthorize("hasAnyRole('USER','ADMIN')") // Allow admins to call chatbot endpoints too
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "User chatbot interactions")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gemini.service.account.path:}")
    private String serviceAccountPath;

    // ⭐ Đã thay đổi sang RestTemplate
    private final RestTemplate restTemplate;
    private final ConversationService conversationService;
    private final RagService ragService;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;
    private final ChatRateLimiter chatRateLimiter;

    /**
     * Main chat endpoint with RAG support (ĐÃ CHUYỂN SANG ĐỒNG BỘ)
     */
    @SuppressWarnings("null")
    @PostMapping
    @Operation(summary = "Ask chatbot", description = "Sends a prompt to the chatbot and returns an answer.")
    @ApiResponse(responseCode = "200", description = "Answer returned")
    @ApiResponse(responseCode = "403", description = "Forbidden or leaked API key")
    @ApiResponse(responseCode = "429", description = "Rate limited")
    // ⭐ Đã bỏ 'Mono'
    public ResponseEntity<String> ask(@Valid @RequestBody ChatRequestDto chatRequest) {
        // Basic guardrails
        if (!StringUtils.hasText(chatRequest.getPrompt())) {
            return badRequest("Prompt trống.", chatRequest.getConversationId());
        }
        if (chatRequest.getPrompt().length() > 4000) { // tránh prompt quá dài gửi lên model
            return badRequest("Prompt quá dài, vui lòng rút ngắn dưới 4000 ký tự.", chatRequest.getConversationId());
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logAuthenticationDetails(auth);

        String conversationId = chatRequest.getConversationId() != null ?
                chatRequest.getConversationId() :
                conversationService.generateConversationId();

        Integer userId = resolveUserId(auth);

        // Basic rate limiting per user to protect external API
        String rateKey = (userId != null) ? ("user:" + userId) : "user:anonymous";
        if (!chatRateLimiter.allow(rateKey)) {
            String errorJson = objectToJson(Map.of(
                "status", "error",
                "error", "Quá nhiều yêu cầu. Vui lòng thử lại sau.",
                "conversationId", conversationId
            ));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-Conversation-Id", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorJson);
        }

        // Check Redis cache for conversation history
        String history = conversationService.getHistoryFromCache(conversationId);
        if (history == null) {
            history = conversationService.buildConversationContext(conversationId);
            conversationService.saveHistoryToCache(conversationId, history);
        }

        // Retrieve relevant context from library database (RAG)
        String libraryContext = ragService.retrieveContext(chatRequest.getPrompt());
        logger.info("📚 RAG Context retrieved: {}", libraryContext);

        // Build context-aware prompt with library knowledge
        String systemPrompt = "Bạn là trợ lý thư viện trường THCS Phương Tú. Hãy trả lời câu hỏi dựa trên thông tin sau:\n\n" + libraryContext + "\n\n";
        String augmentedPrompt = systemPrompt + history + "\nUser: " + chatRequest.getPrompt();

        // Call AI service (existing logic)
        var payload = Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", augmentedPrompt)
                                }
                        )
                }
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Use Gemini API key (public endpoint, no billing required)
        if (!StringUtils.hasText(apiKey)) {
            return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, "❌ GEMINI_API_KEY không được cấu hình. Lấy key miễn phí tại https://aistudio.google.com/app/apikey", conversationId);
        }
        
        String finalUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        HttpEntity<?> requestEntity = new HttpEntity<>(payload, headers);
        try {
            String geminiJsonResponse = executeWithRetry(finalUrl, requestEntity, conversationId);
            String answer = extractAnswer(geminiJsonResponse);

            // Save message and update Redis cache
            conversationService.saveMessage(conversationId, userId, chatRequest.getPrompt(), geminiJsonResponse);
            conversationService.saveHistoryToCache(conversationId, history + "\nUser: " + chatRequest.getPrompt() + "\nAssistant: " + geminiJsonResponse);

            String responseJson = objectToJson(Map.of(
                "status", "ok",
                "conversationId", conversationId,
                "answer", answer
            ));

            logger.info("✅ Chatbot response (length={}): {}", responseJson.length(), responseJson);

            return ResponseEntity.ok()
                .header("X-Conversation-Id", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseJson);

        } catch (HttpClientErrorException.Forbidden e) {
            String body = e.getResponseBodyAsString();
            logger.error("❌ 403 Forbidden from Gemini API. Response: {}", body);
            String msg = (body != null && body.contains("BILLING_DISABLED"))
                    ? "Billing chưa enable trên Google Cloud project. Dùng API key free tier hoặc enable billing."
                    : (body != null && body.contains("reported as leaked"))
                    ? "Gemini API key bị Google báo là đã lộ. Lấy key mới tại https://aistudio.google.com/app/apikey"
                    : "❌ 403 Forbidden. Kiểm tra API key có hợp lệ không.";
            return errorResponse(HttpStatus.FORBIDDEN, msg, conversationId);
        } catch (Exception e) {
            String errorMsg = handleApiError(e);
            logger.error("Chatbot API error: ", e);
            HttpStatusCode statusCode = (e instanceof HttpStatusCodeException) ? ((HttpStatusCodeException) e).getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
            if (statusCode.is5xxServerError()) statusCode = HttpStatus.BAD_GATEWAY; // tránh trả 500 trực tiếp
            return errorResponse(statusCode, errorMsg, conversationId);
        }
    }

    /**
     * Get conversation history
     */
    @SuppressWarnings("null")
    @GetMapping("/history/{conversationId}")
    @Operation(summary = "Get conversation history")
    public ResponseEntity<String> getConversationHistory(@PathVariable String conversationId) {
        // Code này đã đồng bộ, không cần thay đổi
        try {
            var history = conversationService.getConversationHistory(conversationId);
            String body = objectToJson(Map.of(
                "status", "ok",
                "conversationId", conversationId,
                "history", history
            ));
            return ResponseEntity.ok()
                .header("X-Conversation-Id", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        } catch (Exception e) {
            String body = objectToJson(Map.of(
                "status", "error",
                "conversationId", conversationId,
                "error", "Failed to retrieve conversation history"
            ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Conversation-Id", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        }
    }

    /**
     * Get list of user's conversations
     */
    @SuppressWarnings("null")
    @GetMapping("/conversations")
    @Operation(summary = "List user's conversations")
    public ResponseEntity<String> getUserConversations() {
        // Code này đã đồng bộ, không cần thay đổi
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Integer userId = resolveUserId(auth);

            var conversations = conversationService.getUserConversations(userId);
            String body = objectToJson(Map.of(
                "status", "ok",
                "userId", userId,
                "conversations", conversations
            ));
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        } catch (Exception e) {
            String body = objectToJson(Map.of(
                "status", "error",
                "error", "Failed to retrieve conversations"
            ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
        }
    }

    /**
     * Extract user ID from authentication with type safety check
     */
    private Integer resolveUserId(Authentication auth) {
        if (auth == null) {
            logger.warn("Authentication object is null, cannot extract user ID.");
            return null;
        }
        // Try extracting from principal map first (custom token scenario)
        Object principal = auth.getPrincipal();
        if (principal instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) principal;
            Object userId = details.get("userId");
            if (userId instanceof Integer) {
                return (Integer) userId;
            }
        }
        // Fallback: principal is Spring Security User -> look up in DB by username
        String username = auth.getName();
        try {
            Users u = usersRepository.findByUsername(username).orElse(null);
            if (u != null) return u.getUserId();
        } catch (Exception ex) {
            logger.warn("Failed to resolve userId by username: " + username, ex);
        }
        logger.warn("Could not resolve userId for principal username=" + username + " type=" + principal.getClass().getName());
        return null;
    }

    /**
     * Handle different API errors
     */
    private String handleApiError(Throwable e) {
        // ✅ ĐÃ SỬA
        if (e instanceof HttpStatusCodeException) {
            HttpStatusCodeException we = (HttpStatusCodeException) e;
            logger.warn("API Error: {} - {}", we.getStatusCode(), we.getResponseBodyAsString());
            if (we.getStatusCode().value() == 429) {
                return "Too many requests. Please wait a moment and try again.";
            }
            if (we.getStatusCode().value() == 401) {
                return "API key không hợp lệ hoặc hết hạn.";
            }
            if (we.getStatusCode().value() == 404) {
                return "Chatbot model not found. Please contact support. (404)";
            }
            if (we.getStatusCode().value() >= 500) {
                return "AI service is temporarily unavailable. Please try again later.";
            }
        }
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            if (e.getCause().getMessage().contains("timeout")) {
                return "Request timed out. Please try again.";
            }
        }
        return "Xin lỗi, đã xảy ra lỗi khi xử lý yêu cầu. Vui lòng thử lại.";
    }

    private String extractAnswer(String geminiJson) {
        try {
            JsonNode root = objectMapper.readTree(geminiJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText("");
                    if (text != null && !text.isBlank()) return text;
                }
            }
        } catch (Exception ignored) {
        }
        // fallback to raw json if parsing fails
        return geminiJson;
    }

    private String executeWithRetry(String finalUrl, HttpEntity<?> requestEntity, String conversationId) {
        int maxAttempts = 3;
        long backoffMs = 500;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                @SuppressWarnings("null")
                ResponseEntity<String> geminiResponse = restTemplate.exchange(
                        finalUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                if (geminiResponse.getStatusCode().is2xxSuccessful()) {
                    return geminiResponse.getBody();
                }
                if (geminiResponse.getStatusCode().value() == 429 || geminiResponse.getStatusCode().is5xxServerError()) {
                    // retry with backoff
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
                // non-retriable
                throw new HttpClientErrorException(geminiResponse.getStatusCode());
            } catch (HttpStatusCodeException e) {
                if (attempt < maxAttempts && (e.getStatusCode().value() == 429 || e.getStatusCode().is5xxServerError())) {
                    try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    backoffMs *= 2;
                    continue;
                }
                throw e;
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    backoffMs *= 2;
                    continue;
                }
                throw new RuntimeException("Failed after retries: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Failed to call Gemini after retries. conversationId=" + conversationId);
    }

    private ResponseEntity<String> badRequest(String message, String conversationId) {
        return errorResponse(HttpStatus.BAD_REQUEST, message, conversationId);
    }

    @SuppressWarnings("null")
    private ResponseEntity<String> errorResponse(HttpStatusCode status, String message, String conversationId) {
        String errorJson = objectToJson(Map.of(
            "status", "error",
            "error", message,
            "conversationId", conversationId
        ));
        return ResponseEntity.status(status)
                .header("X-Conversation-Id", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorJson);
    }

    private String objectToJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            // Fallback minimal JSON
            return "{\"status\":\"error\",\"error\":\"Serialization failed\"}";
        }
    }

    /**
     * Debugging method to log authentication details
     */
    private void logAuthenticationDetails(Authentication auth) {
        if (auth == null) {
            logger.warn("Authentication object is null.");
            return;
        }
        logger.info("Authentication details: Principal={}, Authorities={}", auth.getPrincipal(), auth.getAuthorities());
    }

    // Logger for this class
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ChatbotController.class);
}