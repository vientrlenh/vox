package com.sep.vox.infrastructure.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.application.port.output.InterestQuizGenerationPort;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

/**
 * Generates the interest quiz (Tier 1) by scenario using AI -- package 13, see
 * task/implement/13-quiz-so-thich-sinh-theo-tinh-huong.md. Exactly the same mould as TopicGenerationClient,
 * only the endpoint/payload differ.
 */
@Service
@Slf4j
public class InterestQuizGenerationClient implements InterestQuizGenerationPort {

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
        String base = agentsBaseUrl.replaceAll("/+$", "");
        this.generateUri = URI.create(base + "/internal/practice-generation/interest-quiz-items");
    }

    /**
     * @param dimensions the current catalogue of interest dimensions (read from interest_dimensions). Sent
     *                   down instead of being hard-coded in Python, so that when a SYSTEM_ADMIN adds a new dimension
     *                   it takes effect immediately, with no need to redeploy the Python service.
     */
    @Override
    public List<InterestQuizSeedItem> generate(
            int maxItems,
            List<String> existingStatements,
            List<String> dimensions) {
        try {
            HttpRequest request = buildGenerateRequest(maxItems, existingStatements, dimensions);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn(
                    "Interest quiz generation endpoint returned status {}: {}",
                    response.statusCode(),
                    response.body()
                );
                return List.of();
            }
            return toItems(response.body());
        } catch (Exception exception) {
            log.warn(
                "Interest quiz generation endpoint unavailable; no items generated",
                exception
            );
            return List.of();
        }
    }

    private HttpRequest buildGenerateRequest(int maxItems, List<String> existingStatements, List<String> dimensions) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("max_items", maxItems);
        body.put("existing_statements", existingStatements);
        body.put("dimensions", dimensions);
        return HttpRequest.newBuilder(generateUri)
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body)))
            .build();
    }

    private List<InterestQuizSeedItem> toItems(String responseBody) throws Exception {
        JsonNode root = jsonMapper.readTree(responseBody);
        List<InterestQuizSeedItem> items = new ArrayList<InterestQuizSeedItem>();
        for (JsonNode node : root.path("items")) {
            List<String> dimensions = new ArrayList<String>();
            for (JsonNode dimension : node.path("dimension_per_statement")) {
                dimensions.add(dimension.asString());
            }
            List<String> statements = new ArrayList<String>();
            for (JsonNode statement : node.path("statements")) {
                statements.add(statement.asString());
            }
            items.add(InterestQuizSeedItem.builder()
                    .id(null)
                    .dimensionPerStatement(dimensions)
                    .statements(statements)
                    .note(node.path("desirability_check").asString())
                    .build());
        }
        return items;
    }
}
