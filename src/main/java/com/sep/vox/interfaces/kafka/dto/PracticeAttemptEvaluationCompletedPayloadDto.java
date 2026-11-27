package com.sep.vox.interfaces.kafka.dto;

import java.util.List;
import java.util.Map;

/**
 * Python grades practice answers with the same graph as assessment grading, so the published payload has the same
 * shape as {@link AssessmentAttemptEvaluationCompletedPayloadDto}. Only the fields that are really used
 * somewhere are declared here -- Jackson ignores the rest.
 */
public record PracticeAttemptEvaluationCompletedPayloadDto(
    ValidityResultDto validity,
    /** Needed to infer pronunciation weaknesses: word_feedback goes down to each phoneme. */
    List<TurnDetailDto> turns,
    Map<String, CriterionScoreDto> criteria,
    EvaluationSignalsDto signals,
    String evaluatedAt
) {
}
