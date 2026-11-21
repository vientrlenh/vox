package com.sep.vox.infrastructure.service;

import java.io.IOException;
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

import com.sep.vox.application.port.output.PracticeQuestionGenerationPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PracticeQuestionGenerationClient implements PracticeQuestionGenerationPort {

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
        String base = agentsBaseUrl.replaceAll("/+$", "");
        this.generationEndpoint = URI.create(
            base + "/internal/practice-generation/questions"
        );
        this.indexEndpoint = URI.create(
            base + "/internal/practice-generation/questions/index"
        );
    }

    /**
     * @param bandCount  the number of bands of the scale in use -- Python uses it to map difficulty_rank onto
     *                    the right scale, instead of defaulting to 6 VSTEP-style bands.
     * @param bandLadder a description of each band, so the grading prompt speaks the school's own scale. When empty
     *                    Python falls back to its own default ladder.
     */
    @Override
    public List<GeneratedQuestion> generate(
            TopicDetails topic,
            String criterionCode,
            String subAttribute,
            String targetTense,
            int targetRank,
            int count,
            Duration timeout,
            int bandCount,
            List<FrameworkResultBand> bandLadder,
            List<UUID> excludeQuestionIds) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("topic_id", topic.id().toString());
            payload.put("topic_name", topic.name());
            // SINGULAR. Python declares `interest_dimension: str` with NO default value
            // (schemas/question_generation.py). If the name is wrong Pydantic silently ignores the unknown field
            // and then reports a missing required field -- every practice question generation call returns 422, and the student
            // only sees an "error" when opening a topic that has no questions in the bank yet.
            payload.put("interest_dimension", topic.interestDimension());
            payload.put("curriculum_group", topic.curriculumGroup());
            payload.put("target_criterion_code", criterionCode);
            payload.put("target_sub_attribute", subAttribute);
            // The grammatical tense the question must force the student to use. The generation prompt is responsible for setting a TIME MARKER
            // so that this tense is the only natural answer -- a bare instruction cannot force it.
            payload.put("target_tense", targetTense);
            payload.put("target_rank", targetRank);
            // Send EXACTLY the configured number. There used to be Math.min(3, count) here -- a second cap
            // that was declared nowhere, silently overriding application.yaml: set 5 and still
            // only get 3, no log, no error. The real cap already exists on the Python side (DRAFTER_CANDIDATES),
            // and there it blows up as a readable 422 error instead of cutting silently.
            payload.put("count", count);
            payload.put("band_count", bandCount);
            // Questions that are permanently dead for this very student -- Python excludes them from the duplicate
            // comparison. If they are not sent, the duplicate gate compares against the whole bank, including questions this student will never
            // see again, and every new draft is thrown away for being "similar to an existing question".
            payload.put(
                "exclude_question_ids",
                (excludeQuestionIds == null ? List.<UUID>of() : excludeQuestionIds)
                    .stream().map(id -> id.toString()).toList()
            );
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
            HttpRequest request = HttpRequest.newBuilder(generationEndpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    jsonMapper.writeValueAsString(payload)
                ))
                .build();
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                    "Question generation endpoint returned "
                        + response.statusCode() + ": " + response.body()
                );
            }
            List<PracticeQuestionGenerationPort.GeneratedQuestion> result = new ArrayList<GeneratedQuestion>();
            for (JsonNode item : jsonMapper.readTree(response.body()).path("questions")) {
                result.add(toQuestion(item));
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Question generation interrupted", exception);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Question generation failed", exception);
        }
    }

    @Override
    public void index(GeneratedQuestion question) {
        try {
            HttpRequest request = HttpRequest.newBuilder(indexEndpoint)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    jsonMapper.writeValueAsString(Map.of(
                        "question", jsonMapper.readTree(question.sourceJson())
                    ))
                ))
                .build();
            HttpResponse<String> response = httpClient.send(
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
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Question indexing failed", exception);
        }
    }

    private GeneratedQuestion toQuestion(JsonNode item) {
        return new GeneratedQuestion(
            UUID.fromString(item.path("id").asString()),
            UUID.fromString(item.path("topic_id").asString()),
            item.path("question_text").asString(),
            item.path("target_criterion_code").asString(),
            item.path("target_sub_attribute").isNull()
                ? null
                : item.path("target_sub_attribute").asString(),
            item.path("target_tense").isNull() || item.path("target_tense").isMissingNode()
                ? null
                : item.path("target_tense").asString(),
            item.path("difficulty_rank").asInt(),
            item.path("difficulty_features").toString(),
            item.path("evaluation_guide").toString(),
            item.path("suggested_ideas").toString(),
            item.path("question_type").asString("SHORT_ANSWER"),
            item.path("max_response_seconds").asInt(),
            item.path("min_response_seconds").asInt(),
            item.path("vstep_part").asInt(),
            item.toString()
        );
    }

}
