package com.sep.vox.application.port.input.command.examevaluation;

import java.util.List;
import java.util.Map;

public record RecordExamAttemptEvaluationPayloadInput(
    List<TurnDetailInput> turns,
    Map<String, CriterionScoreInput> criteria,
    EvaluationSignalsInput signals,
    ValidityResultInput validity,
    String feedbackSummary,
    List<Object> suggestions,
    String modelVersion,
    String promptVersion,
    String evaluatedAt
) {
}
