package com.ibizabroker.lms.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String GEMINI_EMBED_MODEL = "models/text-embedding-004";
    private static final String GEMINI_EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent";

    public List<Double> embed(String text) {
        if (text == null || text.isBlank() || geminiApiKey == null || geminiApiKey.isBlank()) {
            return Collections.emptyList();
        }
        try {
            var payload = new EmbedRequest(GEMINI_EMBED_MODEL, new Content(List.of(new Part(text))));
            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_EMBED_URL + "?key=" + geminiApiKey))
                    .timeout(Duration.ofSeconds(15))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Gemini embed failed: HTTP {} body={}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            EmbedResponse embedResponse = objectMapper.readValue(response.body(), EmbedResponse.class);
            if (embedResponse.embedding() == null || embedResponse.embedding().values() == null) {
                return Collections.emptyList();
            }
            return embedResponse.embedding().values();
        } catch (Exception e) {
            log.error("Gemini embed exception: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // === DTOs ===
    public record EmbedRequest(String model, Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbedResponse(@JsonProperty("embedding") Embedding embedding) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedding(@JsonProperty("values") List<Double> values) {}
}
