package com.sep.vox.infrastructure.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.application.port.output.QuestionDiversityPort;

@Service
public class QuestionDiversityClient implements QuestionDiversityPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        QuestionDiversityClient.class
    );

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI endpoint;

    public QuestionDiversityClient(
            JsonMapper jsonMapper,
            @Value("${PRACTICE_AGENTS_BASE_URL:http://localhost:8000}")
            String agentsBaseUrl) {
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        this.endpoint = URI.create(
            agentsBaseUrl.replaceAll("/+$", "")
                + "/internal/practice-selection/question-similarities"
        );
    }

    @Override
    public Map<UUID, Double> maxSimilarities(
            List<UUID> candidateIds,
            List<UUID> selectedIds) {
        if (selectedIds.isEmpty()) {
            var result = new HashMap<UUID, Double>();
            candidateIds.forEach(id -> result.put(id, 0.0));
            return result;
        }
        try {
            var body = jsonMapper.writeValueAsString(Map.of(
                "candidate_ids",
                candidateIds.stream().map(id -> id.toString()).toList(),
                "selected_ids",
                selectedIds.stream().map(id -> id.toString()).toList()
            ));
            var request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            var response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                    "Similarity endpoint returned " + response.statusCode() + ": " + response.body()
                );
            }
            var values = jsonMapper.readTree(response.body())
                .path("max_similarity");
            var result = new HashMap<UUID, Double>();
            for (var entry : values.properties()) {
                result.put(
                    UUID.fromString(entry.getKey()),
                    entry.getValue().asDouble(1.0)
                );
            }
            return result;
        } catch (IOException | InterruptedException | RuntimeException exception) {
            LOGGER.warn(
                "Question similarity endpoint unavailable; "
                    + "no additional paper item will be selected",
                exception
            );
            return Map.of();
        }
    }

}
