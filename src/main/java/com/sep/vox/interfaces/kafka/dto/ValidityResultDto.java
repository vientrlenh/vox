package com.sep.vox.interfaces.kafka.dto;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

public record ValidityResultDto(
    Boolean validForScoring,
    String action,
    String overallSeverity,
    List<JsonNode> ruleResults,
    List<JsonNode> flags,
    Map<String, Object> scoreCaps,
    List<JsonNode> penalties,
    List<String> notes,
    String transcriptSource,
    Integer transcriptWordCount
) {
}
