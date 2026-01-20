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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            // 1. Convert user query to vector
            List<Double> queryVector = embeddingService.embed(userQuery);
            
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

            // 4. Build context string
            StringBuilder context = new StringBuilder("Thông tin sách trong thư viện:\n");
            books.forEach(book -> {
                context.append("- Tên sách: ").append(book.getName());
                
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
                context.append("- Tên sách: ").append(book.getName());
                
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
     * Ask AI using Gemini API via HTTP call
     */
    @SuppressWarnings("unchecked")
    public String askAi(String userQuery) {
        try {
            if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
                return "Gemini API key not configured. Please set GOOGLE_API_KEY environment variable.";
            }

            String prompt = buildAugmentedPrompt(userQuery);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

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
