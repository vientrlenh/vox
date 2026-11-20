package com.sep.vox.application.response.input.examitemresponse;

import java.util.UUID;

public record ExamItemResponseTurnResponse(
    UUID id,
    UUID examItemResponseId,
    int turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    String answeredAt,
    String createdAt
) {
}
