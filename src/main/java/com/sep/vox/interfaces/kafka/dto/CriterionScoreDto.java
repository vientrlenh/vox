package com.sep.vox.interfaces.kafka.dto;

import java.util.Map;

public record CriterionScoreDto(
    Double score,
    String level,
    String status,
    String source,
    Map<String, Object> subscores,
    String note,
    String suggestion
) {
}
