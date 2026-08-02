package com.sep.vox.interfaces.kafka.dto;

import java.util.Map;

public record PracticeAttemptEvaluationCompletedPayloadDto(
    ValidityResultDto validity,
    Map<String, CriterionScoreDto> criteria,
    EvaluationSignalsDto signals,
    String evaluatedAt
) {
}
