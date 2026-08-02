package com.sep.vox.interfaces.kafka.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CriterionScoreDto(
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
