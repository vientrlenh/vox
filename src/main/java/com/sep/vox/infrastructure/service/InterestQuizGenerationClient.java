package com.sep.vox.infrastructure.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

/**
 * Sinh quiz sở thích (Tier 1) theo tình huống bằng AI -- gói 13, xem
 * task/implement/13-quiz-so-thich-sinh-theo-tinh-huong.md. Mẫu y hệt TopicGenerationClient,
 * chỉ khác endpoint/payload.
 */
@Service
public class InterestQuizGenerationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterestQuizGenerationClient.class);

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI generateUri;

    public InterestQuizGenerationClient(
            JsonMapper jsonMapper,
            @Value("${app.practice.agents-base-url:http://localhost:8000}")
            String agentsBaseUrl) {
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        var base = agentsBaseUrl.replaceAll("/+$", "");
        this.generateUri = URI.create(base + "/internal/practice-generation/interest-quiz-items");
    }

    public List<InterestQuizSeedItem> generate(int maxItems, List<String> existingStatements) {
        var body = new LinkedHashMap<String, Object>();
        body.put("max_items", maxItems);
        body.put("existing_statements", existingStatements);
        try {
            var request = HttpRequest.newBuilder(generateUri)
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body)))
                .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                LOGGER.warn(
                    "Interest quiz generation endpoint returned status {}: {}",
                    response.statusCode(),
                    response.body()
                );
                return List.of();
            }
            return toItems(response.body());
        } catch (Exception exception) {
            LOGGER.warn(
                "Interest quiz generation endpoint unavailable; no items generated",
                exception
            );
            return List.of();
        }
    }

    private List<InterestQuizSeedItem> toItems(String responseBody) throws Exception {
        var root = jsonMapper.readTree(responseBody);
        var items = new ArrayList<InterestQuizSeedItem>();
        for (var node : root.path("items")) {
            var dimensions = new ArrayList<String>();
            for (var dimension : node.path("dimension_per_statement")) {
                dimensions.add(dimension.asText());
            }
            var statements = new ArrayList<String>();
            for (var statement : node.path("statements")) {
                statements.add(statement.asText());
            }
            items.add(new InterestQuizSeedItem(
                null,
                dimensions,
                statements,
                node.path("desirability_check").asText()
            ));
        }
        return items;
    }
}
