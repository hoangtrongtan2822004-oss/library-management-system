package com.ibizabroker.lms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.lms.dao.BooksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * 🤖 RAG Service - Retrieval Augmented Generation
 * 
 * ⚡ Performance Optimization:
 * - ✅ Vector Search: Sử dụng EmbeddingService + PineconeVectorStore thay vì findAll()
 * - ✅ Semantic Search: Tìm theo ngữ nghĩa (cosine similarity) thay vì keyword matching
 * - ✅ Scalable: Xử lý được 10,000+ books mà không tràn RAM
 * 
 * 🎯 Architecture:
 * - retrieveContext: Vector search để tìm sách liên quan
 * - buildAugmentedPrompt: Kết hợp context với user query
 * - askAi: Gọi Gemini API để generate response
 * 
 * 📌 Vector Search Flow:
 * 1. User query → EmbeddingService.embed() → vector representation
 * 2. PineconeVectorStore.query() → top-K similar book vectors
 * 3. Fetch book details từ DB bằng IDs
 * 4. Build context và pass vào Gemini
 * 
 * @author Library Management System
 * @since Phase 6: Service Layer Optimization
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final BooksRepository booksRepository;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final PineconeVectorStore pineconeVectorStore;
    private final AiTaggingService aiTaggingService;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    /**
     * 🔍 Retrieve context using Vector Search (Semantic Search)
     * 
     * ✅ BEFORE (Performance Issue):
     * - Load ALL books from DB → 10,000 books in RAM
     * - Loop through and keyword match → O(n*m)
     * - High memory usage, slow response time
     * 
     * ✅ AFTER (Vector Search):
     * - Convert query to vector → O(1)
     * - Find top-K similar vectors in Pinecone → O(log n)
     * - Fetch only 10 books from DB → O(1)
     * - Memory efficient, fast response
     * 
     * @param userQuery User's question (e.g., "Sách về máy tính")
     * @return Context string with relevant books
     */
    public String retrieveContext(String userQuery) {
        try {
            // 1. Expand query with AI to broaden semantic coverage
            String enrichedQuery = aiTaggingService.expandSearchQuery(userQuery);
            // 2. Convert enriched query to vector
            List<Double> queryVector = embeddingService.embed(enrichedQuery != null ? enrichedQuery : userQuery);
            
            if (queryVector == null || queryVector.isEmpty()) {
                // Fallback: Vector embedding failed, use simple DB query
                return retrieveContextFallback(userQuery);
            }

            // 2. Query Pinecone for top-10 similar books
            List<String> bookIds = pineconeVectorStore.query(queryVector, 10);
            
            if (bookIds == null || bookIds.isEmpty()) {
                return "Không tìm thấy thông tin liên quan trong cơ sở dữ liệu thư viện.";
            }

            // 3. Fetch book details from DB (only 10 books, not ALL)
            List<Integer> intBookIds = bookIds.stream()
                    .map(id -> {
                        try {
                            return Integer.parseInt(id);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(id -> id != null)
                    .toList();

            @SuppressWarnings("null")
            List<com.ibizabroker.lms.entity.Books> books = booksRepository.findAllById(intBookIds);

            if (books == null || books.isEmpty()) {
                return "Không tìm thấy thông tin liên quan trong cơ sở dữ liệu thư viện.";
            }

            // 4. Build context string (include book ID for BOOK_CARD rendering)
            StringBuilder context = new StringBuilder("Thông tin sách trong thư viện:\n");
            books.forEach(book -> {
                context.append("- [ID:").append(book.getId()).append("] Tên sách: ").append(book.getName());
                
                // Add authors if available
                String authorNames = "N/A";
                if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                    authorNames = book.getAuthors().stream()
                            .map(a -> a.getName())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("N/A");
                    context.append(" | Tác giả: ").append(authorNames);
                }
                
                // Add availability
                boolean isAvailable = book.getNumberOfCopiesAvailable() > 0;
                context.append(" | Còn lại: ").append(book.getNumberOfCopiesAvailable()).append(" cuốn");
                
                // Add ISBN
                if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
                    context.append(" | ISBN: ").append(book.getIsbn());
                }
                
                context.append("\n");
            });

            return context.toString();

        } catch (Exception e) {
            // Fallback to simple search if vector search fails
            return retrieveContextFallback(userQuery);
        }
    }

    /**
     * 🔙 Fallback: Simple database search when vector search unavailable
     * 
     * ⚠️ Warning: This method loads all books from DB - use only as fallback
     * 
     * @param userQuery User's question
     * @return Context string with matching books
     */
    private String retrieveContextFallback(String userQuery) {
        try {
            String queryLower = userQuery.toLowerCase();
            
            // Extract keywords from user query (remove common words)
            String[] keywords = queryLower
                    .replaceAll("[?!.,;]", "")
                    .split("\\s+");
            
            // ⚠️ Performance Issue: Loading ALL books from DB
            List<com.ibizabroker.lms.entity.Books> books = booksRepository.findAll().stream()
                    .filter(book -> {
                        String bookNameLower = book.getName().toLowerCase();
                        String isbnLower = book.getIsbn() != null ? book.getIsbn().toLowerCase() : "";
                        
                        // Check if any keyword matches book name or ISBN
                        for (String keyword : keywords) {
                            // Skip common words
                            if (keyword.length() <= 2 || 
                                keyword.equals("sách") || keyword.equals("còn") || 
                                keyword.equals("không") || keyword.equals("lớp") ||
                                keyword.equals("của") || keyword.equals("cho") ||
                                keyword.equals("tìm") || keyword.equals("có")) {
                                continue;
                            }
                            
                            if (bookNameLower.contains(keyword) || isbnLower.contains(keyword)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .limit(10)
                    .toList();

            if (books == null || books.isEmpty()) {
                return "Không tìm thấy thông tin liên quan trong cơ sở dữ liệu thư viện.";
            }

            StringBuilder context = new StringBuilder("Thông tin sách trong thư viện:\n");
            books.forEach(book -> {
                // Include book ID for BOOK_CARD rendering (same as vector search path)
                context.append("- [ID:").append(book.getId()).append("] Tên sách: ").append(book.getName());
                
                // Add authors if available
                if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                    context.append(" | Tác giả: ");
                    context.append(book.getAuthors().stream()
                            .map(a -> a.getName())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("N/A"));
                }
                
                // Add availability
                context.append(" | Còn lại: ").append(book.getNumberOfCopiesAvailable()).append(" cuốn");
                
                // Add ISBN
                if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
                    context.append(" | ISBN: ").append(book.getIsbn());
                }

                // Add description (truncated)
                if (book.getDescription() != null && !book.getDescription().isBlank()) {
                    String desc = book.getDescription().length() > 200
                            ? book.getDescription().substring(0, 200) + "..."
                            : book.getDescription();
                    context.append(" | Mô tả: ").append(desc);
                }

                // Add AI tags
                List<String> tags = aiTaggingService.parseTags(book.getAiTags());
                if (!tags.isEmpty()) {
                    context.append(" | Tags: ").append(String.join(", ", tags));
                }
                
                context.append("\n");
            });

            return context.toString();
        } catch (Exception e) {
            return "Lỗi khi truy xuất dữ liệu: " + e.getMessage();
        }
    }


    /**
     * Build augmented prompt with library context
     */
    public String buildAugmentedPrompt(String userQuery) {
        String context = retrieveContext(userQuery);

        return """
                You are a smart Library Assistant.
                User Question: %s

                Use the following information about books in our library to answer:
                %s

                If the answer is not in the context, politely say you don't know.
                Answer in Vietnamese.
                """.formatted(userQuery, context);
    }

    /**
     * 📸 OCR + AI: Extracts book information from an uploaded cover/TOC image.
     * Uses Gemini Vision (gemini-1.5-flash inlineData) and returns a map with
     * title, authors, isbn, description.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractBookInfoFromImage(MultipartFile image) {
        try {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                return Map.of("error", "Gemini API key not configured");
            }

            byte[] imageBytes = image.getBytes();
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            String prompt = """
                    Phân tích ảnh bìa sách hoặc mục lục này và trích xuất thông tin.
                    Trả về JSON thuần túy (không có markdown, không có ```json), với đúng các trường sau:
                    - title: Tên sách (string)
                    - authors: Danh sách tên tác giả (array of strings)
                    - isbn: Mã ISBN nếu nhìn thấy trong ảnh (string hoặc null)
                    - description: Mô tả ngắn 1-2 câu về nội dung sách (string)
                    Chỉ trả về JSON, không thêm bất kỳ giải thích nào.
                    """;

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> imageData = Map.of("mimeType", mimeType, "data", base64Data);
            Map<String, Object> inlineDataPart = Map.of("inlineData", imageData);
            Map<String, Object> content = Map.of("parts", List.of(textPart, inlineDataPart));
            Map<String, Object> requestBody = Map.of("contents", List.of(content));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

                return httpClient.execute(httpPost, httpResponse -> {
                    int statusCode = httpResponse.getCode();
                    String responseBody = EntityUtils.toString(httpResponse.getEntity());
                    if (statusCode != 200) {
                        // Log raw error for debugging
                        org.slf4j.LoggerFactory.getLogger(RagService.class)
                            .error("Gemini OCR API error {}: {}", statusCode, responseBody);
                        return Map.of("error", "Gemini API lỗi (" + statusCode + "). Vui lòng thử lại.");
                    }
                    try {
                        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            Map<String, Object> candidate = candidates.get(0);
                            Map<String, Object> responseContent = (Map<String, Object>) candidate.get("content");
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) responseContent.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                String text = (String) parts.get(0).get("text");
                                // Strip any markdown code fences Gemini might add
                                text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                                return objectMapper.readValue(text, Map.class);
                            }
                        }
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(RagService.class)
                            .error("OCR parse error. Raw response: {}", responseBody, e);
                        return Map.of("error", "Không thể phân tích phản hồi AI: " + e.getMessage());
                    }
                    return Map.of("error", "Không có phản hồi từ AI");
                });
            }
        } catch (Exception e) {
            return Map.of("error", "Lỗi trích xuất thông tin: " + e.getMessage());
        }
    }

    /**
     * Ask AI using Gemini API via HTTP call
     */
    @SuppressWarnings("unchecked")
    public String askAi(String userQuery) {
        try {
            if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
                return "Gemini API key not configured. Please set GOOGLE_API_KEY environment variable.";
            }

            String prompt = buildAugmentedPrompt(userQuery);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            // Build request payload for Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            
            parts.put("text", prompt);
            List<Map<String, Object>> partsList = List.of(parts);
            contents.put("parts", partsList);
            requestBody.put("contents", List.of(contents));

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            // Make HTTP POST request to Gemini API
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

                var response = httpClient.execute(httpPost, httpResponse -> {
                    String responseBody = EntityUtils.toString(httpResponse.getEntity());
                    
                    // Parse Gemini response
                    try {
                        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            Map<String, Object> candidate = candidates.get(0);
                            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                            List<Map<String, Object>> responseParts = (List<Map<String, Object>>) content.get("parts");
                            if (responseParts != null && !responseParts.isEmpty()) {
                                return (String) responseParts.get(0).get("text");
                            }
                        }
                    } catch (Exception e) {
                        return "Error parsing Gemini response: " + e.getMessage();
                    }
                    
                    return "No response from Gemini API";
                });

                return response;
            }
        } catch (Exception e) {
            return "Error calling Gemini API: " + e.getMessage();
        }
    }
}
