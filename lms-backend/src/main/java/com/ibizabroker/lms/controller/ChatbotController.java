package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ChatRequestDto;
import com.ibizabroker.lms.service.ConversationService;
import com.ibizabroker.lms.service.RagService;
import com.ibizabroker.lms.service.AdvancedSearchService;
import com.ibizabroker.lms.dto.ChatFeedbackRequest;
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
import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dao.ChatFeedbackRepository;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.entity.ChatFeedback;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.lms.service.ChatRateLimiter;
import com.ibizabroker.lms.service.ChatSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Author;
import com.ibizabroker.lms.entity.Category;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private final AdvancedSearchService advancedSearchService;
    private final UsersRepository usersRepository;
    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;
    private final ChatRateLimiter chatRateLimiter;
    private final ChatSecurityService chatSecurityService;
    private final ChatFeedbackRepository chatFeedbackRepository;
    private final BooksRepository booksRepository;
    
    // ThreadPool cho SSE streaming (tránh block main thread)
    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();

    /**
     * 🚀 SSE Streaming Chat Endpoint (Real-time typing effect)
     * 
     * Frontend sử dụng EventSource để nhận real-time response:
     * 
     * const eventSource = new EventSource('/api/user/chat/stream?prompt=' + encodeURIComponent(prompt));
     * eventSource.onmessage = (event) => {
     *     console.log('Received:', event.data);
     *     displayMessage(event.data); // Hiển thị từng chữ như ChatGPT
     * };
     * eventSource.onerror = () => eventSource.close();
     * 
     * 💡 Lợi ích:
     * - User thấy response ngay lập tức (typing effect)
     * - Giảm cảm giác chờ đợi từ 10s xuống 0s
     * - Trải nghiệm giống ChatGPT thật
     */
    @GetMapping("/stream")
    @Operation(
        summary = "Chat với AI (Streaming mode)", 
        description = "Trả về AI response theo kiểu Server-Sent Events để hiển thị typing effect real-time"
    )
    @ApiResponse(responseCode = "200", description = "SSE stream started")
    public SseEmitter streamChat(
            @RequestParam String prompt,
            @RequestParam(required = false) String conversationId,
            HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L); // Timeout 60s

        // Resolve userId & conversationId on the request thread (SecurityContext available here)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = resolveUserId(auth);
        String ip = chatSecurityService.extractIp(request);
        String rateKey = (userId != null) ? ("user:" + userId) : ("ip:" + ip);

        // OWASP A04 — Rate limiting for stream endpoint
        if (!chatRateLimiter.allow(rateKey)) {
            chatSecurityService.logSecurityEvent("RATE_LIMIT_HIT", rateKey, ip, "GET /stream throttled");
            SseEmitter blocked = new SseEmitter();
            try {
                blocked.send(SseEmitter.event().name("error")
                        .data("{\"error\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau.\"}"));
            } catch (IOException ignored) { }
            blocked.complete();
            return blocked;
        }

        // OWASP A03 — Sanitize + injection check (prompt comes in as a GET param)
        String sanitizedPrompt = chatSecurityService.sanitizePrompt(prompt);
        if (sanitizedPrompt.isBlank()) {
            SseEmitter blocked = new SseEmitter();
            try { blocked.send(SseEmitter.event().name("error").data("{\"error\":\"Prompt trống.\"}")); }
            catch (IOException ignored) { }
            blocked.complete();
            return blocked;
        }
        if (chatSecurityService.detectPromptInjection(sanitizedPrompt)) {
            chatSecurityService.logSecurityEvent("PROMPT_INJECTION_ATTEMPT", rateKey, ip,
                    sanitizedPrompt.substring(0, Math.min(100, sanitizedPrompt.length())));
            SseEmitter blocked = new SseEmitter();
            try {
                blocked.send(SseEmitter.event().name("error")
                        .data("{\"error\":\"Prompt chứa nội dung không hợp lệ.\"}"));
            } catch (IOException ignored) { }
            blocked.complete();
            return blocked;
        }

        // OWASP A09 — Anomaly detection (log-only)
        chatSecurityService.detectAnomaly(rateKey, ip);

        String finalConversationId = (conversationId != null && !conversationId.isBlank())
                ? conversationId
                : conversationService.generateConversationId();
        
        streamingExecutor.execute(() -> {
            try {
                // Build augmented prompt with RAG + conversation history (same as ask())
                String libraryContext = ragService.retrieveContext(sanitizedPrompt);
                String statsContext = buildStatisticsContext(sanitizedPrompt);
                if (!statsContext.isEmpty()) {
                    libraryContext = statsContext + "\n\n" + libraryContext;
                }
                String history = conversationService.buildConversationContext(finalConversationId);
                String systemPrompt = "Bạn là trợ lý thư viện trường THCS Phương Tú. Hãy trả lời câu hỏi dựa trên thông tin sau:\n\n" + libraryContext + "\n\n";
                String augmentedPrompt = systemPrompt + history + "\nUser: " + sanitizedPrompt;
                
                emitter.send(SseEmitter.event()
                        .name("start")
                        .data("{\"conversationId\":\"" + finalConversationId + "\"}"));
                
                // Attempt to call Gemini streaming endpoint and forward tokens to client
                if (!StringUtils.hasText(apiKey)) {
                    emitter.send(SseEmitter.event().name("error").data("GEMINI API key not configured"));
                    emitter.complete();
                    return;
                }

                String streamUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=" + apiKey;

                // Accumulate full response for DB save
                StringBuilder fullResponse = new StringBuilder();

                // Build payload with augmented (RAG + history) prompt
                Map<String, Object> streamPayload = Map.of(
                        "contents", new Object[]{
                                Map.of(
                                        "parts", new Object[]{
                                                Map.of("text", augmentedPrompt)
                                        }
                                )
                        }
                );

                HttpURLConnection conn = null;
                try {
                    java.net.URI uri = java.net.URI.create(streamUrl);
                    conn = (HttpURLConnection) uri.toURL().openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "text/event-stream, application/json");
                    conn.setConnectTimeout(15_000);
                    conn.setReadTimeout(0); // streaming: wait indefinitely within emitter timeout

                    String json = objectMapper.writeValueAsString(streamPayload);
                    try (java.io.OutputStream os = conn.getOutputStream()) {
                        os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        os.flush();
                    }

                    int status = conn.getResponseCode();
                    java.io.InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (Thread.currentThread().isInterrupted()) break;
                            if (line.isBlank()) continue;

                            // Many streaming endpoints send SSE-style lines; try to trim "data: " prefix
                            String payloadLine = line;
                            if (payloadLine.startsWith("data: ")) payloadLine = payloadLine.substring(6);

                            String tokenText = null;
                            try {
                                JsonNode node = objectMapper.readTree(payloadLine);
                                // Try several common paths to extract text
                                if (node.has("candidates")) {
                                    JsonNode candidates = node.path("candidates");
                                    if (candidates.isArray() && candidates.size() > 0) {
                                        JsonNode parts = candidates.get(0).path("content").path("parts");
                                        if (parts.isArray() && parts.size() > 0) {
                                            tokenText = parts.get(0).path("text").asText(null);
                                        }
                                    }
                                } else if (node.has("delta")) {
                                    tokenText = node.path("delta").path("content").path("text").asText(null);
                                } else if (node.has("text")) {
                                    tokenText = node.path("text").asText(null);
                                }
                            } catch (Exception ex) {
                                // not JSON, use raw payloadLine
                            }

                            String toSend = (tokenText != null && !tokenText.isBlank()) ? tokenText : payloadLine;
                            String safeData = (toSend != null) ? toSend : "";
                            fullResponse.append(safeData);
                            emitter.send(SseEmitter.event().name("message").data(safeData));
                        }
                    }

                    // Save streamed conversation to DB
                    conversationService.saveMessage(finalConversationId, userId, sanitizedPrompt, fullResponse.toString());

                    emitter.send(SseEmitter.event().name("end").data("{\"status\":\"completed\"}"));
                    emitter.complete();

                } catch (Exception ex) {
                    logger.error("Error streaming from Gemini: {}", ex.getMessage(), ex);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("Streaming error: " + ex.getMessage()));
                    } catch (IOException ioe) {
                        // ignore
                    }
                    emitter.completeWithError(ex);
                } finally {
                    if (conn != null) conn.disconnect();
                }
                
            } catch (IOException e) {
                logger.error("SSE streaming error: ", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

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
    public ResponseEntity<String> ask(@Valid @RequestBody ChatRequestDto chatRequest,
                                       HttpServletRequest request) {
        // Basic guardrails
        if (!StringUtils.hasText(chatRequest.getPrompt())) {
            return badRequest("Prompt trống.", chatRequest.getConversationId());
        }
        if (chatRequest.getPrompt().length() > 4000) {
            return badRequest("Prompt quá dài, vui lòng rút ngắn dưới 4000 ký tự.", chatRequest.getConversationId());
        }

        // OWASP A03 — Sanitize input and detect prompt-injection attacks
        String ip = chatSecurityService.extractIp(request);
        String sanitizedPrompt = chatSecurityService.sanitizePrompt(chatRequest.getPrompt());
        if (chatSecurityService.detectPromptInjection(sanitizedPrompt)) {
            chatSecurityService.logSecurityEvent("PROMPT_INJECTION_ATTEMPT", "pre-auth", ip,
                    sanitizedPrompt.substring(0, Math.min(100, sanitizedPrompt.length())));
            return badRequest("Prompt chứa nội dung không hợp lệ.", chatRequest.getConversationId());
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logAuthenticationDetails(auth);

        String conversationId = chatRequest.getConversationId() != null ?
                chatRequest.getConversationId() :
                conversationService.generateConversationId();

        Integer userId = resolveUserId(auth);

        // OWASP A04 — Rate limiting per user (falls back to IP for unauthenticated)
        String rateKey = (userId != null) ? ("user:" + userId) : ("ip:" + ip);
        if (!chatRateLimiter.allow(rateKey)) {
            chatSecurityService.logSecurityEvent("RATE_LIMIT_HIT", rateKey, ip, "POST /chat throttled");
            String errorJson = objectToJson(Map.of(
                "status", "error",
                "error", "Quá nhiều yêu cầu. Vui lòng thử lại sau.",
                "conversationId", conversationId
            ));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-Conversation-Id", conversationId)
                .header("Retry-After", String.valueOf(chatRateLimiter.getRetryAfterSeconds(rateKey)))
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body(errorJson);
        }

        // OWASP A09 — Anomaly detection (log-only; does not block)
        chatSecurityService.detectAnomaly(rateKey, ip);

        // Check Redis cache for conversation history
        String history = conversationService.getHistoryFromCache(conversationId);
        if (history == null) {
            history = conversationService.buildConversationContext(conversationId);
            conversationService.saveHistoryToCache(conversationId, history);
        }

        // Retrieve relevant context from library database (RAG)
        String libraryContext = ragService.retrieveContext(sanitizedPrompt);
        logger.info("📚 RAG Context retrieved: {}", libraryContext);

        // Inject real statistics context for loan/statistics queries
        String statsContext = buildStatisticsContext(sanitizedPrompt);
        if (!statsContext.isEmpty()) {
            libraryContext = statsContext + "\n\n" + libraryContext;
            logger.info("📊 Statistics Context injected: {}", statsContext);
        }

        // Build context-aware prompt with library knowledge
        String systemPrompt = "Bạn là trợ lý thư viện trường THCS Phương Tú. Hãy trả lời câu hỏi dựa trên thông tin sau:\n\n" + libraryContext + "\n\n";
        String augmentedPrompt = systemPrompt + history + "\nUser: " + sanitizedPrompt;

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
            conversationService.saveMessage(conversationId, userId, sanitizedPrompt, geminiJsonResponse);
            conversationService.saveHistoryToCache(conversationId, history + "\nUser: " + sanitizedPrompt + "\nAssistant: " + geminiJsonResponse);

            var suggestionsCandidate = advancedSearchService.getSearchSuggestions(sanitizedPrompt, 5);
            var suggestions = (suggestionsCandidate != null && !suggestionsCandidate.isEmpty())
                    ? suggestionsCandidate
                    : java.util.List.of(
                        "Sách Toán lớp 6 còn không?",
                        "Hướng dẫn mượn sách",
                        "Giờ mở cửa",
                        "Sách tham khảo Ngữ văn",
                        "Top sách mượn nhiều"
                    );

            String responseJson = objectToJson(Map.of(
                "status", "ok",
                "conversationId", conversationId,
                "answer", answer,
                "suggestions", suggestions
            ));

            logger.info("✅ Chatbot response (length={}): {}", responseJson.length(), responseJson);

            return ResponseEntity.ok()
                .header("X-Conversation-Id", conversationId)
                .header("X-Content-Type-Options", "nosniff")
                .header("X-RateLimit-Remaining", String.valueOf(chatRateLimiter.getRemainingRequests(rateKey)))
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
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
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8)) // ✅ Force UTF-8
                .body(body);
        } catch (Exception e) {
            String body = objectToJson(Map.of(
                "status", "error",
                "conversationId", conversationId,
                "error", "Failed to retrieve conversation history"
            ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Conversation-Id", conversationId)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8)) // ✅ Force UTF-8
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
            Map<String, String> titles = conversationService.getTitlesForUser(userId);
            String body = objectToJson(Map.of(
                "status", "ok",
                "userId", userId,
                "conversations", conversations,
                "titles", titles
            ));
            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8)) // ✅ Force UTF-8
                .body(body);
        } catch (Exception e) {
            String body = objectToJson(Map.of(
                "status", "error",
                "error", "Failed to retrieve conversations"
            ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8)) // ✅ Force UTF-8
                .body(body);
        }
    }

    /**
     * Rename a conversation by updating its stored title.
     */
    @SuppressWarnings("null")
    @PatchMapping("/conversations/{conversationId}/rename")
    @Operation(summary = "Rename a conversation")
    public ResponseEntity<String> renameConversation(
            @PathVariable String conversationId,
            @RequestBody java.util.Map<String, String> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Integer userId = resolveUserId(auth);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                    .body("{\"status\":\"error\",\"error\":\"User not authenticated\"}");
            }
            String title = body.get("title");
            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest()
                    .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                    .body("{\"status\":\"error\",\"error\":\"Title is required\"}");
            }
            String safeTitle = title.strip();
            boolean renamed = conversationService.renameConversation(conversationId, userId, safeTitle);
            if (!renamed) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                    .body("{\"status\":\"error\",\"error\":\"Conversation not found or access denied\"}");
            }
            String escaped = safeTitle.replace("\\", "\\\\").replace("\"", "\\\"");
            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body("{\"status\":\"ok\",\"conversationId\":\"" + conversationId + "\",\"title\":\"" + escaped + "\"}");
        } catch (Exception e) {
            logger.error("❌ Error renaming conversation {}: {}", conversationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body("{\"status\":\"error\",\"error\":\"Failed to rename conversation\"}");
        }
    }

    /**
     * Delete a specific conversation (all messages) for the authenticated user
     */
    @SuppressWarnings("null")
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<String> deleteConversation(@PathVariable String conversationId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Integer userId = resolveUserId(auth);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                    .body("{\"status\":\"error\",\"error\":\"User not authenticated\"}");
            }

            boolean deleted = conversationService.deleteConversation(conversationId, userId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                    .body("{\"status\":\"error\",\"error\":\"Conversation not found or access denied\"}");
            }

            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body("{\"status\":\"ok\",\"deleted\":\"" + conversationId + "\"}");
        } catch (Exception e) {
            logger.error("❌ Error deleting conversation {}: {}", conversationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body("{\"status\":\"error\",\"error\":\"Failed to delete conversation\"}");
        }
    }

    /**
     * 📸 OCR: Extract book info from uploaded cover image using Gemini Vision
     */
    @SuppressWarnings("null")
    @PostMapping("/ocr")
    @Operation(summary = "Extract book info from cover image", description = "Uploads a book cover image and uses AI Vision to extract title, authors, ISBN, description")
    @ApiResponse(responseCode = "200", description = "Book info extracted")
    public ResponseEntity<String> extractBookInfo(@RequestParam("image") org.springframework.web.multipart.MultipartFile image) {
        try {
            if (image.isEmpty()) {
                return badRequest("Vui lòng chọn ảnh để tải lên.", "ocr");
            }
            
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return badRequest("File phải là ảnh (JPEG, PNG, v.v.).", "ocr");
            }

            if (image.getSize() > 10 * 1024 * 1024) { // 10MB max
                return badRequest("Ảnh quá lớn. Vui lòng chọn ảnh dưới 10MB.", "ocr");
            }

            Map<String, Object> result = ragService.extractBookInfoFromImage(image);
            
            if (result.containsKey("error")) {
                Object errVal = result.get("error");
                String errMsg = errVal instanceof String ? (String) errVal : errVal.toString();
                return errorResponse(HttpStatus.BAD_REQUEST, errMsg, "ocr");
            }

            String responseJson = objectToJson(Map.of(
                "status", "ok",
                "bookInfo", result
            ));

            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body(responseJson);

        } catch (Exception e) {
            logger.error("❌ OCR error: ", e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý ảnh: " + e.getMessage(), "ocr");
        }
    }

    /**
     * Reading Insight: AI phân tích thói quen đọc sách của người dùng
     */
    @GetMapping("/reading-insight")
    @Operation(summary = "AI phân tích thói quen đọc sách", description = "Gemini tạo nhận xét cá nhân hóa dựa trên lịch sử mượn sách của người dùng")
    @SuppressWarnings("null")
    public ResponseEntity<String> getReadingInsight() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = resolveUserId(auth);
        if (userId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng", "reading-insight");
        }

        try {
            // Lấy danh sách bookIds từ lịch sử mượn
            List<Integer> bookIds = loanRepository.findDistinctBookIdsByMemberId(userId);
            if (bookIds == null || bookIds.isEmpty()) {
                String body = objectToJson(Map.of(
                    "status", "ok",
                    "insight", "Bạn chưa mượn sách nào. Hãy khám phá thư viện và bắt đầu hành trình đọc sách của mình nhé! 📚"
                ));
                return ResponseEntity.ok()
                    .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                    .body(body);
            }

            // Lấy tối đa 20 cuốn sách gần nhất
            List<Integer> limitedIds = bookIds.stream().limit(20).toList();
            List<Books> loanedBooks = booksRepository.findAllById(limitedIds);

            // Thống kê thể loại và tác giả yêu thích
            Map<String, Long> categoryCount = loanedBooks.stream()
                .filter(b -> b.getCategories() != null)
                .flatMap(b -> b.getCategories().stream())
                .collect(Collectors.groupingBy(Category::getName, Collectors.counting()));

            Map<String, Long> authorCount = loanedBooks.stream()
                .filter(b -> b.getAuthors() != null)
                .flatMap(b -> b.getAuthors().stream())
                .collect(Collectors.groupingBy(Author::getName, Collectors.counting()));

            String topCategories = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));

            String topAuthors = authorCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));

            String bookList = loanedBooks.stream()
                .limit(10)
                .map(Books::getName)
                .collect(Collectors.joining(", "));

            // Xây dựng prompt cho Gemini
            String prompt = String.format(
                "Bạn là chuyên gia tư vấn đọc sách. Hãy phân tích thói quen đọc sách của người dùng và đưa ra nhận xét cá nhân hóa bằng tiếng Việt.\n\n" +
                "Thông tin người dùng:\n" +
                "- Tổng số sách đã mượn: %d cuốn\n" +
                "- Thể loại yêu thích: %s\n" +
                "- Tác giả đọc nhiều nhất: %s\n" +
                "- Một số sách đã đọc: %s\n\n" +
                "Hãy viết 3-4 câu nhận xét thú vị về sở thích đọc sách, điểm mạnh về thể loại, và gợi ý hướng phát triển. " +
                "Giọng văn thân thiện, khích lệ. Không liệt kê bullet points.",
                bookIds.size(), topCategories, topAuthors, bookList
            );

            // Gọi Gemini
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", 300, "temperature", 0.8)
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String geminiJson = executeWithRetry(url, requestEntity, "reading-insight");
            String insight = extractAnswer(geminiJson);

            String responseBody = objectToJson(Map.of(
                "status", "ok",
                "insight", insight,
                "stats", Map.of(
                    "totalBooks", bookIds.size(),
                    "topCategories", topCategories,
                    "topAuthors", topAuthors
                )
            ));
            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body(responseBody);

        } catch (Exception e) {
            logger.error("❌ Reading insight error: ", e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi phân tích thói quen đọc sách: " + e.getMessage(), "reading-insight");
        }
    }

        /**
         * Feedback endpoint for chatbot responses
         */
        @PostMapping("/feedback")
        @Operation(summary = "Send feedback for chatbot response")
        @SuppressWarnings("null") // MediaType/Charset constants lack @NonNull; usage is safe
        public ResponseEntity<String> feedback(@Valid @RequestBody ChatFeedbackRequest feedback) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = resolveUserId(auth);

        logger.info("Chatbot feedback: userId={}, conversationId={}, messageId={}, helpful={} reason={}",
            userId, feedback.getConversationId(), feedback.getMessageId(), feedback.isHelpful(), feedback.getReason());

        // Persist feedback to database
        ChatFeedback entity = new ChatFeedback(
            feedback.getConversationId(),
            feedback.getMessageId(),
            userId,
            feedback.isHelpful(),
            feedback.getReason()
        );
        chatFeedbackRepository.save(entity);

        String body = objectToJson(Map.of(
            "status", "ok",
            "conversationId", feedback.getConversationId(),
            "messageId", feedback.getMessageId(),
            "helpful", feedback.isHelpful()
        ));

        return ResponseEntity.ok()
            .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
            .body(body);
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
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
                // non-retriable
                throw new HttpClientErrorException(geminiResponse.getStatusCode());
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 429) {
                    // Parse retryDelay from Gemini body (e.g. "retryDelay": "40s")
                    long retryDelaySec = parseGeminiRetryDelaySec(e.getResponseBodyAsString());
                    // If Gemini asks us to wait more than 10 s, fail-fast so the
                    // caller can serve a DB-only fallback instead of blocking.
                    if (retryDelaySec > 10) {
                        throw e;
                    }
                    if (attempt < maxAttempts) {
                        long waitMs = Math.max(backoffMs, retryDelaySec * 1000);
                        try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        backoffMs = waitMs * 2;
                        continue;
                    }
                    throw e;
                }
                if (attempt < maxAttempts && e.getStatusCode().is5xxServerError()) {
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

    /** Extract retryDelay seconds from a Gemini 429 body ("retryDelay": "40s" → 40). */
    private long parseGeminiRetryDelaySec(String body) {
        if (body == null || body.isBlank()) return 0;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode details = root.path("error").path("details");
            if (details.isArray()) {
                for (JsonNode detail : details) {
                    String delay = detail.path("retryDelay").asText("");
                    if (!delay.isBlank()) {
                        return Long.parseLong(delay.replaceAll("[^0-9]", ""));
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
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
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8)) // ✅ Force UTF-8
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

    /**
     * Get AI-powered book recommendations based on user's borrowing history
     */
    @SuppressWarnings("null")
    @PostMapping("/recommend-books")
    @Operation(summary = "Get personalized book recommendations", description = "Uses AI to recommend books based on user's borrowing history")
    @ApiResponse(responseCode = "200", description = "Recommendations returned")
    public ResponseEntity<String> getBookRecommendations() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = resolveUserId(auth);
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":\"error\",\"error\":\"User not authenticated\"}");
            }

            // 1. Lấy lịch sử mượn thực tế từ DB
            List<Integer> borrowedBookIds = loanRepository.findDistinctBookIdsByMemberId(userId);

            StringBuilder historyContext = new StringBuilder();
            String ragQuery = "sách hay được độc giả quan tâm"; // fallback query

            if (borrowedBookIds != null && !borrowedBookIds.isEmpty()) {
                // Giới hạn 15 cuốn gần nhất để prompt không quá dài
                List<Integer> recentIds = borrowedBookIds.stream().limit(15).toList();
                List<Books> borrowedBooks = booksRepository.findAllById(recentIds);

                if (!borrowedBooks.isEmpty()) {
                    historyContext.append("Người dùng đã từng mượn các sách sau:\n");
                    for (Books b : borrowedBooks) {
                        historyContext.append("- \"").append(b.getName()).append("\"");
                        if (b.getAuthors() != null && !b.getAuthors().isEmpty()) {
                            historyContext.append(" (tác giả: ")
                                .append(b.getAuthors().stream().map(Author::getName).collect(Collectors.joining(", ")))
                                .append(")");
                        }
                        if (b.getCategories() != null && !b.getCategories().isEmpty()) {
                            historyContext.append(" [thể loại: ")
                                .append(b.getCategories().stream().map(Category::getName).collect(Collectors.joining(", ")))
                                .append("]");
                        }
                        historyContext.append("\n");
                    }

                    // Xây dựng RAG query từ thể loại yêu thích của user
                    String preferredGenres = borrowedBooks.stream()
                        .flatMap(b -> b.getCategories() == null
                            ? java.util.stream.Stream.empty()
                            : b.getCategories().stream())
                        .map(Category::getName)
                        .distinct()
                        .limit(5)
                        .collect(Collectors.joining(" "));

                    ragQuery = preferredGenres.isBlank()
                        ? borrowedBooks.stream().map(Books::getName).limit(3).collect(Collectors.joining(" "))
                        : preferredGenres;
                }
            } else {
                historyContext.append("Người dùng chưa có lịch sử mượn sách.\n");
            }

            // 2. Truy vấn Pinecone với query có ngữ nghĩa thực tế
            String libraryContext = ragService.retrieveContext(ragQuery);

            // 3. Build Gemini prompt với đầy đủ ngữ cảnh
            String prompt = "Dựa trên danh sách sách trong thư viện:\n\n" + libraryContext +
                "\n\n---\n" + historyContext +
                "\nHãy gợi ý 6 cuốn sách phù hợp từ danh sách thư viện trên cho người dùng này. " +
                "Ưu tiên sách cùng thể loại hoặc tác giả mà người dùng đã đọc, nhưng chưa có trong lịch sử mượn. " +
                "Trả về CHÍNH XÁC định dạng JSON (không markdown, không text thêm):\n" +
                "[{\"id\": <bookId hoặc null nếu không biết>, \"title\": \"Tên sách\", \"reason\": \"Lý do ngắn gọn\"}, ...]" +
                "\nLưu ý: lấy bookId từ [ID:xx] trong ngữ cảnh thư viện nếu có.";

            var payload = Map.of(
                "contents", new Object[]{
                    Map.of(
                        "parts", new Object[]{
                            Map.of("text", prompt)
                        }
                    )
                }
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (!StringUtils.hasText(apiKey)) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":\"error\",\"error\":\"GEMINI_API_KEY not configured\"}");
            }
            
            String finalUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            HttpEntity<?> requestEntity = new HttpEntity<>(payload, headers);

            String geminiResponse = executeWithRetry(finalUrl, requestEntity, "recommendations-" + userId);
            String answer = extractAnswer(geminiResponse);
            
            logger.info("✨ AI Recommendations for user {}: {}", userId, answer);

            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body("{\"status\":\"ok\",\"userId\":" + userId + ",\"recommendations\":" + answer + "}");

        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 429) {
                logger.warn("⚠️ Gemini quota exceeded for recommendations (user {}), serving DB fallback", userId);
                return buildDbFallbackRecommendations(userId);
            }
            logger.error("❌ Error generating recommendations (HTTP {}): {}", e.getStatusCode().value(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"error\",\"error\":\"Failed to generate recommendations\"}");
        } catch (Exception e) {
            logger.error("❌ Error generating recommendations: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"error\",\"error\":\"Failed to generate recommendations\"}");
        }
    }

    /**
     * DB-only fallback when Gemini quota is exceeded.
     * Returns books from the same categories the user has borrowed, excluding already-read titles.
     */
    @SuppressWarnings("null")
    private ResponseEntity<String> buildDbFallbackRecommendations(Integer userId) {
        try {
            List<Integer> borrowedIds = loanRepository.findDistinctBookIdsByMemberId(userId);
            if (borrowedIds == null) borrowedIds = List.of();

            // Collect category IDs from borrowed books
            final List<Integer> safeIds = borrowedIds;
            List<Integer> categoryIds = booksRepository.findAllById(safeIds).stream()
                .flatMap(b -> b.getCategories() == null
                    ? java.util.stream.Stream.empty()
                    : b.getCategories().stream())
                .map(Category::getId)
                .distinct()
                .toList();

            List<Books> candidates;
            if (!categoryIds.isEmpty()) {
                candidates = booksRepository.findByCategoryIdsExcludingBookIds(
                    categoryIds,
                    borrowedIds.isEmpty() ? List.of(-1) : borrowedIds,
                    PageRequest.of(0, 6)
                );
            } else {
                candidates = booksRepository.findByNumberOfCopiesAvailableGreaterThan(0)
                    .stream().limit(6).toList();
            }

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < candidates.size(); i++) {
                Books b = candidates.get(i);
                String reason = !categoryIds.isEmpty()
                    ? "Cùng thể loại sách bạn đã đọc"
                    : "Sách phổ biến trong thư viện";
                json.append("{\"id\":").append(b.getId())
                    .append(",\"title\":\"").append(b.getName().replace("\"", "\\\""))
                    .append("\",\"reason\":\"").append(reason).append("\"}");
                if (i < candidates.size() - 1) json.append(",");
            }
            json.append("]");

            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body("{\"status\":\"ok\",\"fallback\":true,\"userId\":" + userId + ",\"recommendations\":" + json + "}");
        } catch (Exception ex) {
            logger.error("❌ DB fallback recommendations failed: ", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8))
                .body("{\"status\":\"error\",\"error\":\"Dịch vụ gợi ý tạm thời không khả dụng. Vui lòng thử lại sau.\"}");
        }
    }

    // Logger for this class
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ChatbotController.class);

    /**
     * Detect statistics/ranking intents in the user query and inject real DB data.
     * Returns an empty string if the query is not statistics-related.
     */
    private String buildStatisticsContext(String query) {
        if (query == null) return "";
        String q = query.toLowerCase();

        // Keywords for "top borrowed books"
        boolean isMostBorrowed = q.contains("mượn nhiều") || q.contains("muon nhieu")
                || q.contains("top sách") || q.contains("phổ biến nhất") || q.contains("popular")
                || q.contains("best seller") || q.contains("bestseller")
                || q.contains("được mượn nhiều") || q.contains("sách hot")
                || q.contains("hay được mượn") || q.contains("mượn nhiều nhất");

        // Keywords for top borrowers
        boolean isTopBorrowers = q.contains("độc giả") && (q.contains("mượn nhiều") || q.contains("tích cực"))
                || q.contains("người mượn nhiều") || q.contains("top user")
                || q.contains("top người dùng") || q.contains("mượn nhiều nhất");

        StringBuilder ctx = new StringBuilder();

        if (isMostBorrowed) {
            try {
                var topBooks = loanRepository.findMostLoanedBooks(PageRequest.of(0, 10));
                if (topBooks != null && !topBooks.isEmpty()) {
                    ctx.append("Thống kê Top 10 sách được mượn nhiều nhất trong thư viện:\n");
                    int rank = 1;
                    for (var entry : topBooks) {
                        Object bookName = entry.get("bookName");
                        Object loanCount = entry.get("loanCount");
                        ctx.append(rank).append(". ").append(bookName)
                           .append(" — đã được mượn ").append(loanCount).append(" lần\n");
                        rank++;
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not fetch most loaned books for chatbot context: {}", e.getMessage());
            }
        }

        if (isTopBorrowers) {
            try {
                var topBorrowers = loanRepository.findTopBorrowers(PageRequest.of(0, 5));
                if (topBorrowers != null && !topBorrowers.isEmpty()) {
                    ctx.append("\nTop 5 độc giả mượn sách nhiều nhất:\n");
                    int rank = 1;
                    for (var entry : topBorrowers) {
                        Object userName = entry.get("userName");
                        Object loanCount = entry.get("loanCount");
                        ctx.append(rank).append(". ").append(userName)
                           .append(" — ").append(loanCount).append(" lần mượn\n");
                        rank++;
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not fetch top borrowers for chatbot context: {}", e.getMessage());
            }
        }

        return ctx.toString();
    }
}