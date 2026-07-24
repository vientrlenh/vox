package com.sep.vox.interfaces.kafka.dto;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

public record ExamAttemptEvaluationCompletedPayloadDto(
    List<TurnDetailDto> turns,
    Map<String, CriterionScoreDto> criteria,
    EvaluationSignalsDto signals,
    ValidityResultDto validity,
    String feedbackSummary,
    List<JsonNode> suggestions,
    String modelVersion,
    String promptVersion,
    String evaluatedAt
) {
}
