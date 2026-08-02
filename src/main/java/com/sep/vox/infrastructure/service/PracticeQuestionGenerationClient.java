package com.sep.vox.infrastructure.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.framework.FrameworkResultBand;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PracticeQuestionGenerationClient {

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI generationEndpoint;
    private final URI indexEndpoint;

    public PracticeQuestionGenerationClient(
            JsonMapper jsonMapper,
            @Value("${PRACTICE_AGENTS_BASE_URL:http://localhost:8000}")
            String agentsBaseUrl) {
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();
        var base = agentsBaseUrl.replaceAll("/+$", "");
        this.generationEndpoint = URI.create(
            base + "/internal/practice-generation/questions"
        );
        this.indexEndpoint = URI.create(
            base + "/internal/practice-generation/questions/index"
        );
    }

    /**
     * @param bandCount  số bậc của thang đang áp -- Python dùng để ánh xạ difficulty_rank ra
     *                   đúng thang, thay vì mặc định 6 bậc kiểu VSTEP.
     * @param bandLadder mô tả từng bậc, để prompt chấm nói đúng thang của trường. Rỗng thì
     *                   Python tự lùi về ladder mặc định của nó.
     */
    public List<GeneratedQuestion> generate(
            TopicDetails topic,
            String criterionCode,
            String subAttribute,
            int targetRank,
            int count,
            Duration timeout,
            int bandCount,
            List<FrameworkResultBand> bandLadder) {
        try {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("topic_id", topic.id().toString());
            payload.put("topic_name", topic.name());
            payload.put("interest_dimension", topic.interestDimension());
            payload.put("curriculum_group", topic.curriculumGroup());
            payload.put("target_criterion_code", criterionCode);
            payload.put("target_sub_attribute", subAttribute);
            payload.put("target_rank", targetRank);
            payload.put("count", Math.min(3, count));
            payload.put("band_count", bandCount);
            payload.put(
                "band_ladder",
                (bandLadder == null ? List.<FrameworkResultBand>of() : bandLadder).stream()
                    .map(rung -> Map.of(
                        "order", rung.getOrder(),
                        "code", rung.getCode() == null ? "" : rung.getCode(),
                        "description",
                        rung.getDescription() == null ? "" : rung.getDescription()
                    ))
                    .toList()
            );
            var request = HttpRequest.newBuilder(generationEndpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    jsonMapper.writeValueAsString(payload)
                ))
                .build();
            var response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                    "Question generation endpoint returned "
                        + response.statusCode() + ": " + response.body()
                );
            }
            var result = new ArrayList<GeneratedQuestion>();
            for (var item : jsonMapper.readTree(response.body()).path("questions")) {
                result.add(toQuestion(item));
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Question generation interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Question generation failed", exception);
        }
    }

    public void index(GeneratedQuestion question) {
        try {
            var request = HttpRequest.newBuilder(indexEndpoint)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    jsonMapper.writeValueAsString(Map.of(
                        "question", question.sourceJson()
                    ))
                ))
                .build();
            var response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                    "Question index endpoint returned " + response.statusCode() + ": " + response.body()
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Question indexing interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Question indexing failed", exception);
        }
    }

    private GeneratedQuestion toQuestion(JsonNode item) {
        return new GeneratedQuestion(
            UUID.fromString(item.path("id").asText()),
            UUID.fromString(item.path("topic_id").asText()),
            item.path("question_text").asText(),
            item.path("target_criterion_code").asText(),
            item.path("target_sub_attribute").isNull()
                ? null
                : item.path("target_sub_attribute").asText(),
            item.path("difficulty_rank").asInt(),
            item.path("difficulty_features").toString(),
            item.path("evaluation_guide").toString(),
            item.path("suggested_ideas").toString(),
            item.path("preparation_time_seconds").asInt(),
            item.path("max_response_seconds").asInt(),
            item.path("max_followup_seconds").asInt(),
            item.path("vstep_part").asInt(),
            item
        );
    }

    public record TopicDetails(
        UUID id,
        String name,
        String interestDimension,
        String curriculumGroup
    ) {
    }

    public record GeneratedQuestion(
        UUID id,
        UUID topicId,
        String questionText,
        String criterionCode,
        String subAttribute,
        int difficultyRank,
        String difficultyFeaturesJson,
        String evaluationGuideJson,
        String suggestedIdeasJson,
        int preparationTimeSeconds,
        int maxResponseSeconds,
        int maxFollowupSeconds,
        int vstepPart,
        JsonNode sourceJson
    ) {
    }
}
