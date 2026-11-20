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
    /** Cần cho việc suy điểm yếu phát âm: word_feedback đi tới từng âm vị. */
    List<TurnDetailDto> turns,
    Map<String, CriterionScoreDto> criteria,
    EvaluationSignalsDto signals,
    String evaluatedAt
) {
}
