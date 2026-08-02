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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import com.sep.vox.application.event.HumanGradingSubmittedEvent;

/**
 * Suy nhãn điểm yếu (sub-attribute) từ feedbackSummary giáo viên chấm tay -- gọi Python
 * (agents) vì taxonomy nhãn (ALLOWED_WEAKNESS_LABELS) và LLM sống ở đó. Mẫu y hệt
 * InterestQuizGenerationClient/TopicGenerationClient, chỉ khác endpoint/payload.
 */
@Service
public class GradingDiagnosticsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradingDiagnosticsClient.class);

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI inferUri;

    public GradingDiagnosticsClient(
            JsonMapper jsonMapper,
            @Value("${app.practice.agents-base-url:http://localhost:8000}")
            String agentsBaseUrl) {
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        var base = agentsBaseUrl.replaceAll("/+$", "");
        this.inferUri = URI.create(base + "/internal/grading-diagnostics/infer");
    }

    public List<InferredLabel> infer(List<HumanGradingSubmittedEvent.Item> items) {
        var itemsById = new LinkedHashMap<String, HumanGradingSubmittedEvent.Item>();
        var requestItems = new ArrayList<Map<String, Object>>();
        for (var item : items) {
            if (item.criteria().isEmpty()
                    || item.feedbackSummary() == null
                    || item.feedbackSummary().isBlank()) {
                continue;
            }
            var itemId = item.evaluationId().toString();
            itemsById.put(itemId, item);
            var body = new LinkedHashMap<String, Object>();
            body.put("item_id", itemId);
            body.put("feedback_summary", item.feedbackSummary());
            body.put("criteria", item.criteria().stream()
                .map(HumanGradingSubmittedEvent.CriterionRef::code)
                .toList());
            requestItems.add(body);
        }
        if (requestItems.isEmpty()) {
            return List.of();
        }

        try {
            var payload = Map.of("items", requestItems);
            var request = HttpRequest.newBuilder(inferUri)
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(payload)))
                .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                LOGGER.warn(
                    "Grading diagnostics endpoint returned status {}: {}",
                    response.statusCode(),
                    response.body()
                );
                return List.of();
            }
            return toLabels(response.body(), itemsById);
        } catch (Exception exception) {
            LOGGER.warn("Grading diagnostics endpoint unavailable; no labels inferred", exception);
            return List.of();
        }
    }

    private List<InferredLabel> toLabels(
            String responseBody,
            Map<String, HumanGradingSubmittedEvent.Item> itemsById) throws Exception {
        var root = jsonMapper.readTree(responseBody);
        var out = new ArrayList<InferredLabel>();
        for (var itemNode : root.path("items")) {
            var sourceItem = itemsById.get(itemNode.path("item_id").asText());
            if (sourceItem == null) {
                continue;
            }
            var criterionByCode = sourceItem.criteria().stream()
                .collect(Collectors.toMap(
                    HumanGradingSubmittedEvent.CriterionRef::code, Function.identity(), (left, right) -> left));
            for (var labelNode : itemNode.path("labels")) {
                var criterionRef = criterionByCode.get(labelNode.path("criterion_code").asText());
                if (criterionRef == null) {
                    continue;
                }
                out.add(new InferredLabel(
                    sourceItem.evaluationId(),
                    criterionRef.frameworkCriterionId(),
                    criterionRef.code(),
                    labelNode.path("label").asText(),
                    labelNode.path("evidence_span").asText("")
                ));
            }
        }
        return out;
    }

    public record InferredLabel(
        UUID evaluationId,
        UUID frameworkCriterionId,
        String criterionCode,
        String label,
        String evidenceSpan
    ) {
    }
}
