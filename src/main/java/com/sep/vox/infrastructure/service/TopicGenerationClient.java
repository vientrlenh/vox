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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.domain.service.personalization.TensePolicy;

@Service
public class TopicGenerationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        TopicGenerationClient.class
    );

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI proposeUri;
    private final URI indexUri;

    public TopicGenerationClient(
            JsonMapper jsonMapper,
            @Value("${app.practice.agents-base-url:http://localhost:8000}")
            String agentsBaseUrl) {
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        var base = agentsBaseUrl.replaceAll("/+$", "");
        this.proposeUri = URI.create(
            base + "/internal/practice-generation/topics"
        );
        this.indexUri = URI.create(
            base + "/internal/practice-generation/topics/index"
        );
    }

    /**
     * @param dimensions danh mục chiều sở thích hiện hành (đọc từ interest_dimension) -- gửi
     *                   xuống để Python ràng buộc đầu ra theo đúng danh mục đang có, thay vì
     *                   gắn cứng và phải deploy lại mỗi lần admin thêm chiều mới.
     */
    public List<TopicProposal> propose(
            UUID studentId,
            List<KeywordEvidence> keywordEvidence,
            Map<String, Double> interestScores,
            List<String> existingTopics,
            List<String> rejectedTopics,
            List<String> exhaustedTopics,
            boolean searchKeyword,
            int maxProposals,
            List<String> dimensions) {
        var body = new LinkedHashMap<String, Object>();
        body.put("student_id", studentId.toString());
        body.put("dimensions", dimensions);
        body.put(
            "keyword_evidence",
            keywordEvidence.stream()
                .map(item -> Map.of(
                    "keyword", item.keyword(),
                    "session_count", item.sessionCount()
                ))
                .toList()
        );
        body.put("interest_scores", interestScores);
        body.put("existing_topics", existingTopics);
        body.put("rejected_topics", rejectedTopics);
        body.put("exhausted_topics", exhaustedTopics);
        body.put("search_keyword", searchKeyword);
        body.put("max_proposals", maxProposals);
        try {
            var response = send(proposeUri, body);
            if (response.statusCode() / 100 != 2) {
                LOGGER.warn(
                    "Topic proposal endpoint returned status {}: {}",
                    response.statusCode(),
                    response.body()
                );
                return List.of();
            }
            var root = jsonMapper.readTree(response.body());
            var proposals = new ArrayList<TopicProposal>();
            for (var node : root.path("proposals")) {
                proposals.add(new TopicProposal(
                    node.path("name").asText(),
                    node.path("interest_dimension").asText(),
                    node.path("curriculum_group").asText(),
                    node.path("temporal_affordance").asString(TensePolicy.AFFORDANCE_MIXED),
                    node.path("confidence").asDouble(),
                    node.path("reason_text").asText(),
                    node.path("evidence_type").asText(),
                    jsonMapper.writeValueAsString(
                        Map.of(
                            "evidence_type",
                            node.path("evidence_type").asText(),
                            "evidence_keywords",
                            node.path("evidence_keywords"),
                            "distinct_from",
                            node.path("distinct_from").asText(),
                            "grounded_in_keyword",
                            node.path("grounded_in_keyword").asBoolean()
                        )
                    )
                ));
            }
            return proposals;
        } catch (Exception exception) {
            LOGGER.warn(
                "Topic proposal endpoint unavailable; no topic was generated",
                exception
            );
            return List.of();
        }
    }

    public void index(
            String topicId,
            String name,
            String description,
            boolean active,
            UUID studentId,
            String status) {
        var body = new LinkedHashMap<String, Object>();
        body.put("topic_id", topicId);
        body.put("name", name);
        body.put("description", description == null ? "" : description);
        body.put("active", active);
        body.put(
            "student_id",
            studentId == null ? null : studentId.toString()
        );
        body.put("status", status);
        try {
            var response = send(indexUri, body);
            if (response.statusCode() / 100 != 2) {
                LOGGER.warn(
                    "Topic index endpoint returned status {} for {}: {}",
                    response.statusCode(),
                    topicId,
                    response.body()
                );
            }
        } catch (Exception exception) {
            LOGGER.warn(
                "Topic index endpoint unavailable for {}",
                topicId,
                exception
            );
        }
    }

    private HttpResponse<String> send(
            URI uri,
            Map<String, Object> body) throws Exception {
        var request = HttpRequest.newBuilder(uri)
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

    public record KeywordEvidence(String keyword, int sessionCount) {
    }

    public record TopicProposal(
            String name,
            String interestDimension,
            String curriculumGroup,
            String temporalAffordance,
            double confidence,
            String reasonText,
            String evidenceType,
            String evidenceJson) {
    }
}
