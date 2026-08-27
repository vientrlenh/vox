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

import com.sep.vox.application.port.output.TopicGenerationPort;
import com.sep.vox.domain.service.personalization.TensePolicy;

@Service
public class TopicGenerationClient implements TopicGenerationPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        TopicGenerationClient.class
    );

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
        var base = agentsBaseUrl.replaceAll("/+$", "");
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
        // GỠ 2026-08-11: existing_topics. Trước đây gửi TOÀN BỘ tên chủ đề đang hoạt động, mà kho
        // là của chung mọi học sinh nên nó lớn dần mãi -- token vào tăng theo (số học sinh × số
        // chủ đề mỗi người sinh). Python nay dùng vòng đề xuất lại có phản hồi: vòng 1 đề xuất tự
        // do, vòng sau nhận đúng TÊN chủ đề vừa va chạm. Xem MAX_PROPOSAL_ROUNDS bên đó.
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
                    node.path("name").asString(),
                    node.path("interest_dimensions").asString(),
                    node.path("curriculum_group").asString(),
                    node.path("temporal_affordance").asString(TensePolicy.AFFORDANCE_MIXED),
                    node.path("confidence").asDouble(),
                    node.path("reason_text").asString(),
                    node.path("evidence_type").asString(),
                    jsonMapper.writeValueAsString(
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

    @Override
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

    /**
     * Tìm chủ đề gần nghĩa với từ khoá. Trả về id kèm độ tương đồng, KHÔNG trả tên.
     *
     * <p>Lỗi hoặc quá hạn thì trả rỗng, không ném. Đây là nguồn BỔ SUNG cho tìm theo chuỗi:
     * agents chết thì người dùng vẫn có kết quả từ Postgres và không thấy lỗi gì -- suy giảm êm
     * thay vì hỏng cả ô tìm kiếm.
     */
    @Override
    public List<TopicSearchHit> searchByVector(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("keyword", keyword.strip());
        body.put("limit", limit);
        try {
            var response = send(searchUri, body);
            if (response.statusCode() / 100 != 2) {
                LOGGER.warn(
                    "Topic search endpoint returned status {}: {}",
                    response.statusCode(),
                    response.body()
                );
                return List.of();
            }
            var hits = new ArrayList<TopicSearchHit>();
            for (var node : jsonMapper.readTree(response.body()).path("hits")) {
                hits.add(new TopicSearchHit(
                    UUID.fromString(node.path("topic_id").asString()),
                    node.path("similarity").asDouble()
                ));
            }
            return hits;
        } catch (Exception exception) {
            LOGGER.warn("Topic search endpoint unavailable; falling back to name search only",
                exception);
            return List.of();
        }
    }

}
