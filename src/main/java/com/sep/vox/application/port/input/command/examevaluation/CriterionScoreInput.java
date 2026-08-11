package com.sep.vox.application.port.input.command.examevaluation;

import java.util.List;
import java.util.Map;

public record CriterionScoreInput(
    Double score,
    String level,
    String status,
    String source,
    Map<String, Object> subscores,
    String note,
    String suggestion,
    List<String> weaknessLabels,
    List<String> evidenceSpans,
    String recommendationTag,
    String matchedBandCode
) {
}
