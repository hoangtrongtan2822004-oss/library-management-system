package com.ibizabroker.lms.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PineconeVectorStore implements VectorStore {

    private final ObjectMapper objectMapper;

    @Value("${pinecone.api.key:}")
    private String apiKey;

    @Value("${pinecone.index.url:}")
    private String indexUrl; // e.g., https://<index-name>-<env>.svc.<region>.pinecone.io

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Override
    public void upsert(String id, List<Double> vector, Map<String, Object> metadata) {
        if (!isConfigured() || vector == null || vector.isEmpty()) return;
        try {
            var payload = Map.of(
                    "vectors", List.of(Map.of(
                            "id", id,
                            "values", vector,
                            "metadata", metadata == null ? Collections.emptyMap() : metadata
                    )),
                    "namespace", "books"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(indexUrl + "/vectors/upsert"))
                    .timeout(Duration.ofSeconds(15))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("Api-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<String> query(List<Double> vector, int topK) {
        if (!isConfigured() || vector == null || vector.isEmpty()) return Collections.emptyList();
        try {
            var payload = Map.of(
                    "vector", vector,
                    "topK", topK,
                    "namespace", "books",
                    "includeMetadata", false
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(indexUrl + "/query"))
                    .timeout(Duration.ofSeconds(15))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("Api-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Collections.emptyList();
            }
            QueryResponse qr = objectMapper.readValue(response.body(), QueryResponse.class);
            if (qr.matches() == null) return Collections.emptyList();
            return qr.matches().stream()
                    .map(Match::id)
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void delete(String id) {
        if (!isConfigured() || id == null || id.isBlank()) return;
        try {
            var payload = Map.of(
                    "ids", List.of(id),
                    "namespace", "books"
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(indexUrl + "/vectors/delete"))
                    .timeout(Duration.ofSeconds(10))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("Api-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && indexUrl != null && !indexUrl.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueryResponse(@JsonProperty("matches") List<Match> matches) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Match(@JsonProperty("id") String id) {}
}
