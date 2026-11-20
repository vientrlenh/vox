package com.sep.vox.interfaces.rest.dto.request;

public record UpdateExamItemResponseTurnRequest(
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    String answeredAt
) {
}
