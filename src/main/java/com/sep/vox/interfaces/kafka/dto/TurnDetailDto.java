package com.sep.vox.interfaces.kafka.dto;

import java.util.List;

import tools.jackson.databind.JsonNode;

public record TurnDetailDto(
    String turnId,
    Integer turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer wordCount,
    Integer durationSeconds,
    Double asrConfidence,
    PronunciationOverallDto pronunciationOverall,
    List<JsonNode> wordFeedback
) {
}
