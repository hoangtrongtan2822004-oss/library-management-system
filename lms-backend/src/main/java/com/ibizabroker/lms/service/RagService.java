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

@Service
@RequiredArgsConstructor
public class RagService {

    private final BooksRepository booksRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    /**
     * Retrieve context from database using simple text search
     */
    public String retrieveContext(String userQuery) {
        try {
            String queryLower = userQuery.toLowerCase();
            
            // Extract keywords from user query (remove common words)
            String[] keywords = queryLower
                    .replaceAll("[?!.,;]", "")
                    .split("\\s+");
            
            // Simple text search in database
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
