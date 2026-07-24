package com.sep.vox.application.port.input.command.examevaluation;

import java.util.Map;

public record CriterionScoreInput(
    Double score,
    String level,
    String status,
    String source,
    Map<String, Object> subscores,
    String note,
    String suggestion
) {
}
