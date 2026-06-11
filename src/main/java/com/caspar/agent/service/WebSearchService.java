package com.caspar.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用联网检索服务（无鉴权版）。
 * 当前默认使用 DuckDuckGo Instant Answer API，适合快速补充通用事实信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private final ObjectMapper objectMapper;

    @Value("${agent.web-search.enabled:true}")
    private boolean enabled;

    @Value("${agent.web-search.timeout-ms:6000}")
    private int timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public List<Map<String, String>> search(String query, int limit) {
        if (!enabled || query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://api.duckduckgo.com/?q=" + encoded
                    + "&format=json&no_redirect=1&no_html=1&skip_disambig=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 2000)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("联网检索失败，HTTP状态码={}", response.statusCode());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<Map<String, String>> results = new ArrayList<>();

            appendIfPresent(results, limit, "摘要", root.path("AbstractText").asText(null), root.path("AbstractURL").asText(null));
            appendIfPresent(results, limit, "答案", root.path("Answer").asText(null), root.path("AbstractURL").asText(null));
            appendIfPresent(results, limit, "定义", root.path("Definition").asText(null), root.path("DefinitionURL").asText(null));

            JsonNode relatedTopics = root.path("RelatedTopics");
            if (relatedTopics.isArray()) {
                collectRelatedTopics(relatedTopics, results, limit);
            }

            return results;
        } catch (Exception e) {
            log.warn("联网检索异常: {}", e.getMessage());
            return List.of();
        }
    }

    private void collectRelatedTopics(JsonNode topics, List<Map<String, String>> results, int limit) {
        for (JsonNode node : topics) {
            if (results.size() >= limit) {
                return;
            }

            // DuckDuckGo 的 RelatedTopics 可能是叶子，也可能有嵌套 Topics
            if (node.has("Topics") && node.get("Topics").isArray()) {
                collectRelatedTopics(node.get("Topics"), results, limit);
                continue;
            }

            String text = node.path("Text").asText(null);
            String url = node.path("FirstURL").asText(null);
            appendIfPresent(results, limit, "相关", text, url);
        }
    }

    private void appendIfPresent(List<Map<String, String>> results, int limit, String title, String snippet, String url) {
        if (results.size() >= limit) {
            return;
        }
        if (snippet == null || snippet.isBlank()) {
            return;
        }
        Map<String, String> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("snippet", snippet);
        item.put("url", (url == null || url.isBlank()) ? "" : url);
        results.add(item);
    }
}

