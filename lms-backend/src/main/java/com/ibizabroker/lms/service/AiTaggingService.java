package com.ibizabroker.lms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🤖 AI Auto-Tagging Service
 *
 * When an admin adds/updates a book with a description, this service calls Gemini
 * to automatically extract relevant Vietnamese tags (e.g. ["Kỹ năng sống", "Công nghệ"]).
 *
 * Output is stored as a JSON array string in Books.aiTags column.
 * Falls back gracefully to empty tags if Gemini is unavailable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaggingService {

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String GEMINI_GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    /**
     * Async: generate tags for a book and return as JSON string.
     * Callers should store the result in Books.aiTags.
     */
    @Async
    public CompletableFuture<String> generateTagsAsync(String bookName, String description, List<String> existingCategories) {
        return CompletableFuture.supplyAsync(() -> generateTags(bookName, description, existingCategories));
    }

    public String generateTags(String bookName, String description, List<String> existingCategories) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.debug("[AiTagging] GEMINI_API_KEY not configured, skipping auto-tagging");
            return "[]";
        }
        if (description == null || description.isBlank()) {
            return "[]";
        }
        try {
            String categories = existingCategories.isEmpty() ? "(không có)" : String.join(", ", existingCategories);
            String prompt = """
                Dựa vào thông tin sách sau, hãy sinh ra 3-7 tags ngắn gọn bằng tiếng Việt.
                Trả về ĐÚNG định dạng JSON array, không có text nào khác.
                Ví dụ: ["Kỹ năng sống","Giao tiếp","Phát triển bản thân"]

                Tên sách: %s
                Thể loại hiện tại: %s
                Mô tả: %s
                """.formatted(bookName, categories, description.substring(0, Math.min(description.length(), 800)));

            String requestBody = """
                {
                  "contents": [{"parts": [{"text": %s}]}],
                  "generationConfig": {"temperature": 0.3, "maxOutputTokens": 200}
                }
                """.formatted(objectMapper.writeValueAsString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_GENERATE_URL + "?key=" + geminiApiKey))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("[AiTagging] Gemini returned HTTP {}: {}", response.statusCode(), response.body());
                return "[]";
            }

            // Parse Gemini response: candidates[0].content.parts[0].text
            var root = objectMapper.readTree(response.body());
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("");

            // Extract JSON array from text (Gemini might wrap in ```json blocks)
            String cleaned = text.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();
            int start = cleaned.indexOf('[');
            int end   = cleaned.lastIndexOf(']');
            if (start == -1 || end == -1) return "[]";
            String jsonArray = cleaned.substring(start, end + 1);

            // Validate it's a proper JSON array
            List<String> tags = objectMapper.readValue(jsonArray, new TypeReference<>() {});
            log.info("[AiTagging] Generated {} tags for book '{}': {}", tags.size(), bookName, tags);
            return objectMapper.writeValueAsString(tags);

        } catch (Exception e) {
            log.error("[AiTagging] Failed to generate tags for book '{}': {}", bookName, e.getMessage());
            return "[]";
        }
    }

    /**
     * Parse aiTags JSON string back to a List<String>.
     */
    public List<String> parseTags(String aiTagsJson) {
        if (aiTagsJson == null || aiTagsJson.isBlank() || aiTagsJson.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(aiTagsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 📝 Generate a Vietnamese book description (150-300 words) using Gemini.
     * Called from admin panel "Generate Description" button.
     *
     * @return Generated description string, or null if Gemini unavailable.
     */
    public String generateDescription(String bookName, List<String> authors, List<String> categories) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) return null;
        try {
            String authorsStr   = authors.isEmpty()    ? "(chưa rõ)" : String.join(", ", authors);
            String categoriesStr = categories.isEmpty() ? "(chưa rõ)" : String.join(", ", categories);

            String prompt = """
                Viết một đoạn mô tả sách ngắn gọn, hấp dẫn bằng tiếng Việt (150-250 từ) cho cuốn sách sau.
                Chỉ trả về đoạn mô tả thuần túy, không có tiêu đề, không có markdown.

                Tên sách: %s
                Tác giả: %s
                Thể loại: %s
                """.formatted(bookName, authorsStr, categoriesStr);

            String requestBody = """
                {
                  "contents": [{"parts": [{"text": %s}]}],
                  "generationConfig": {"temperature": 0.7, "maxOutputTokens": 400}
                }
                """.formatted(objectMapper.writeValueAsString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_GENERATE_URL + "?key=" + geminiApiKey))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            var root = objectMapper.readTree(response.body());
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("").trim();
            return text.isBlank() ? null : text;
        } catch (Exception e) {
            log.error("[AiTagging] generateDescription failed for '{}': {}", bookName, e.getMessage());
            return null;
        }
    }

    /**
     * 🛡️ Moderate a review comment: returns true if content is acceptable,
     * false if it contains hate speech, spam, or inappropriate content.
     * Falls back to true (allow) if Gemini is unavailable (fail-open).
     */
    public boolean moderateReview(String comment) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || comment == null || comment.isBlank()) {
            return true; // fail-open
        }
        try {
            String prompt = """
                Kiểm tra nội dung đánh giá sách sau đây. Trả về ĐÚNG MỘT TỪ: "OK" hoặc "TOXIC".
                - "TOXIC" nếu: ngôn ngữ thù địch, xúc phạm, spam, quảng cáo, hoặc không liên quan đến sách.
                - "OK" nếu: nội dung hợp lệ dù là tích cực hay tiêu cực.

                Đánh giá: %s
                """.formatted(comment.substring(0, Math.min(comment.length(), 500)));

            String requestBody = """
                {
                  "contents": [{"parts": [{"text": %s}]}],
                  "generationConfig": {"temperature": 0.0, "maxOutputTokens": 10}
                }
                """.formatted(objectMapper.writeValueAsString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_GENERATE_URL + "?key=" + geminiApiKey))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return true;

            var root = objectMapper.readTree(response.body());
            String verdict = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("OK").trim().toUpperCase();
            log.info("[AiModeration] Review verdict='{}' for comment (first 60 chars): '{}'",
                    verdict, comment.substring(0, Math.min(60, comment.length())));
            return !verdict.contains("TOXIC");
        } catch (Exception e) {
            log.warn("[AiModeration] Moderation failed, defaulting to approve: {}", e.getMessage());
            return true; // fail-open
        }
    }

    /**
     * 🔍 Expand a user search query into richer semantic terms for better Pinecone retrieval.
     * E.g.: "sách lập trình" → "lập trình phần mềm kỹ thuật công nghệ Java Python thuật toán"
     * Falls back to original query if Gemini unavailable.
     */
    public String expandSearchQuery(String query) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || query == null || query.isBlank()) {
            return query;
        }
        try {
            String prompt = """
                Mở rộng câu truy vấn tìm kiếm sách sau thành 8-12 từ khóa liên quan bằng tiếng Việt.
                Chỉ trả về danh sách từ khóa cách nhau bằng dấu cách, không có dấu phẩy, không có markdown.
                Ví dụ: "sách kỹ năng sống tư duy thành công phát triển bản thân tích cực"

                Câu truy vấn gốc: %s
                """.formatted(query.substring(0, Math.min(query.length(), 200)));

            String requestBody = """
                {
                  "contents": [{"parts": [{"text": %s}]}],
                  "generationConfig": {"temperature": 0.2, "maxOutputTokens": 60}
                }
                """.formatted(objectMapper.writeValueAsString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_GENERATE_URL + "?key=" + geminiApiKey))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return query;

            var root = objectMapper.readTree(response.body());
            String expanded = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("").trim()
                    .replaceAll("[,\\.\\[\\]\"']", "")
                    .replaceAll("\\s+", " ");
            if (expanded.isBlank()) return query;
            // Combine original + expanded for best recall
            return query + " " + expanded;
        } catch (Exception e) {
            log.warn("[AiTagging] expandSearchQuery failed, using original: {}", e.getMessage());
            return query;
        }
    }
}
