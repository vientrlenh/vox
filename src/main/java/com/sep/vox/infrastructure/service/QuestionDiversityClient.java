package com.sep.vox.infrastructure.service;

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

@Service
public class QuestionDiversityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        QuestionDiversityClient.class
    );

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI endpoint;
    private final URI neighborEndpoint;

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
        this.neighborEndpoint = URI.create(
            agentsBaseUrl.replaceAll("/+$", "")
                + "/internal/practice-selection/neighbor-questions"
        );
    }

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
                candidateIds.stream().map(UUID::toString).toList(),
                "selected_ids",
                selectedIds.stream().map(UUID::toString).toList()
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
        } catch (Exception exception) {
            LOGGER.warn(
                "Question similarity endpoint unavailable; "
                    + "no additional paper item will be selected",
                exception
            );
            return Map.of();
        }
    }

    public List<NeighborQuestion> neighborQuestions(
            String topicName,
            String criterionCode,
            int rankMin,
            int rankMax) {
        try {
            var body = jsonMapper.writeValueAsString(Map.of(
                "topic_name", topicName,
                "criterion_code", criterionCode,
                "rank_min", rankMin,
                "rank_max", rankMax,
                "limit", 50
            ));
            var request = HttpRequest.newBuilder(neighborEndpoint)
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
                    "Neighbor endpoint returned " + response.statusCode() + ": " + response.body()
                );
            }
            var questions = jsonMapper.readTree(response.body())
                .path("questions");
            var result = new java.util.ArrayList<NeighborQuestion>();
            for (var question : questions) {
                result.add(new NeighborQuestion(
                    UUID.fromString(question.path("question_id").asText()),
                    question.path("similarity").asDouble()
                ));
            }
            return result;
        } catch (Exception exception) {
            LOGGER.warn(
                "Neighbor question endpoint unavailable; "
                    + "paper generation will continue with the next tier",
                exception
            );
            return List.of();
        }
    }

    public List<UUID> neighborQuestionIds(
            String topicName,
            String criterionCode,
            int rankMin,
            int rankMax) {
        return neighborQuestions(
            topicName,
            criterionCode,
            rankMin,
            rankMax
        ).stream().map(NeighborQuestion::questionId).toList();
    }

    public record NeighborQuestion(UUID questionId, double similarity) {
    }
}
