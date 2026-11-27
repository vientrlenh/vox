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
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.application.port.output.TopicGenerationPort;
import com.sep.vox.domain.service.personalization.TensePolicy;

@Service
@Slf4j
public class TopicGenerationClient implements TopicGenerationPort {

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI proposeUri;
    private final URI indexUri;
    private final URI searchUri;

    public TopicGenerationClient(
            JsonMapper jsonMapper,
            @Value("${app.practice.agents-base-url:http://localhost:8000}")
            String agentsBaseUrl) {
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        String base = agentsBaseUrl.replaceAll("/+$", "");
        this.proposeUri = URI.create(
            base + "/internal/practice-generation/topics"
        );
        this.indexUri = URI.create(
            base + "/internal/practice-generation/topics/index"
        );
        this.searchUri = URI.create(
            base + "/internal/practice-generation/topics/search"
        );
    }

    @Override
    public List<TopicProposal> propose(
            UUID studentId,
            List<KeywordEvidence> keywordEvidence,
            Map<String, Double> interestScores,
            List<String> rejectedTopics,
            List<String> exhaustedTopics,
            boolean searchKeyword,
            int maxProposals,
            List<String> dimensions) {
        Map<String, Object> body = buildProposeBody(studentId, keywordEvidence, interestScores,
            rejectedTopics, exhaustedTopics, searchKeyword, maxProposals, dimensions);
        try {
            HttpResponse<String> response = send(proposeUri, body);
            if (!isSuccessful("Topic proposal", response)) {
                return List.of();
            }
            return parseProposals(response.body());
        } catch (Exception exception) {
            log.warn("Topic proposal endpoint unavailable; no topic was generated", exception);
            return List.of();
        }
    }

    private Map<String, Object> buildProposeBody(
            UUID studentId, List<KeywordEvidence> keywordEvidence, Map<String, Double> interestScores,
            List<String> rejectedTopics, List<String> exhaustedTopics, boolean searchKeyword,
            int maxProposals, List<String> dimensions) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("student_id", studentId.toString());
        body.put("dimensions", dimensions);
        body.put("keyword_evidence", toKeywordEvidencePayload(keywordEvidence));
        body.put("interest_scores", interestScores);
        // REMOVED 2026-08-11: existing_topics. It used to send ALL the names of active topics, and since the bank
        // is shared by every student it only kept growing -- input tokens grew with it (number of students x number of
        // topics each one generates). Python now uses a propose-again loop with feedback: round 1 proposes
        // freely, later rounds receive exactly the NAME of the topic that just collided. See MAX_PROPOSAL_ROUNDS there.
        body.put("rejected_topics", rejectedTopics);
        body.put("exhausted_topics", exhaustedTopics);
        body.put("search_keyword", searchKeyword);
        body.put("max_proposals", maxProposals);
        return body;
    }

    private List<Map<String, Object>> toKeywordEvidencePayload(List<KeywordEvidence> keywordEvidence) {
        return keywordEvidence.stream()
            .map(item -> Map.<String, Object>of(
                "keyword", item.keyword(),
                "session_count", item.sessionCount()
            ))
            .toList();
    }

    private List<TopicProposal> parseProposals(String responseBody) {
        JsonNode root = jsonMapper.readTree(responseBody);
        List<TopicGenerationPort.TopicProposal> proposals = new ArrayList<TopicProposal>();
        for (JsonNode node : root.path("proposals")) {
            proposals.add(toProposal(node));
        }
        return proposals;
    }

    private TopicProposal toProposal(JsonNode node) {
        return new TopicProposal(
            node.path("name").asString(),
            // SINGULAR. Python returns `interest_dimension` (schemas/topic_generation.py:45).
            // If the name is read wrong Jackson returns a MissingNode and asString() gives an EMPTY STRING --
            // no error thrown, no log. That empty string is written straight into
            // practice_topics.interest_dimensions and stays there permanently, then blows up late at the
            // question generation step: Python rejects with 422 `string_too_short` because the topic has no interest
            // dimension. The error surfaces two service layers away from where it was caused.
            node.path("interest_dimension").asString(),
            node.path("curriculum_group").asString(),
            node.path("temporal_affordance").asString(TensePolicy.AFFORDANCE_MIXED),
            node.path("confidence").asDouble(),
            node.path("reason_text").asString(),
            node.path("evidence_type").asString(),
            toEvidenceJson(node)
        );
    }

    private String toEvidenceJson(JsonNode node) {
        return jsonMapper.writeValueAsString(
            Map.of(
                "evidence_type",
                node.path("evidence_type").asString(),
                "evidence_keywords",
                node.path("evidence_keywords"),
                "distinct_from",
                node.path("distinct_from").asString(),
                "grounded_in_keyword",
                node.path("grounded_in_keyword").asBoolean()
            )
        );
    }

    private boolean isSuccessful(String endpointName, HttpResponse<String> response) {
        if (response.statusCode() / 100 == 2) {
            return true;
        }
        log.warn("{} endpoint returned status {}: {}", endpointName, response.statusCode(), response.body());
        return false;
    }

    @Override
    public void index(
            String topicId, String name, String description, boolean active, UUID studentId, String status) {
        Map<String, Object> body = buildIndexBody(topicId, name, description, active, studentId, status);
        try {
            HttpResponse<String> response = send(indexUri, body);
            if (response.statusCode() / 100 != 2) {
                log.warn(
                    "Topic index endpoint returned status {} for {}: {}",
                    response.statusCode(),
                    topicId,
                    response.body()
                );
            }
        } catch (Exception exception) {
            log.warn(
                "Topic index endpoint unavailable for {}",
                topicId,
                exception
            );
        }
    }

    private Map<String, Object> buildIndexBody(
            String topicId, String name, String description, boolean active, UUID studentId, String status) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("topic_id", topicId);
        body.put("name", name);
        body.put("description", description == null ? "" : description);
        body.put("active", active);
        body.put(
            "student_id",
            studentId == null ? null : studentId.toString()
        );
        body.put("status", status);
        return body;
    }

    private HttpResponse<String> send(
            URI uri,
            Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    jsonMapper.writeValueAsString(body)
                )
            )
            .build();
        return httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
    }

    /**
     * Find topics close in meaning to the keyword. Returns the id with the similarity, NOT the name.
     *
     * <p>On error or timeout it returns empty, it does not throw. This is a SUPPLEMENTARY source to string search:
     * if agents die the user still gets results from Postgres and sees no error -- graceful degradation
     * instead of breaking the whole search box.
     */
    @Override
    public List<TopicSearchHit> searchByVector(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("keyword", keyword.strip());
        body.put("limit", limit);
        try {
            HttpResponse<String> response = send(searchUri, body);
            if (!isSuccessful("Topic search", response)) {
                return List.of();
            }
            return parseSearchHits(response.body());
        } catch (Exception exception) {
            log.warn("Topic search endpoint unavailable; falling back to name search only",
                exception);
            return List.of();
        }
    }

    private List<TopicSearchHit> parseSearchHits(String responseBody) {
        List<TopicGenerationPort.TopicSearchHit> hits = new ArrayList<TopicSearchHit>();
        for (JsonNode node : jsonMapper.readTree(responseBody).path("hits")) {
            hits.add(new TopicSearchHit(
                UUID.fromString(node.path("topic_id").asString()),
                node.path("similarity").asDouble()
            ));
        }
        return hits;
    }

}
