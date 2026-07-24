package com.sep.vox.application.response.input.examitemresponse;

import java.util.UUID;

public record ExamItemEvaluationTurnResponse(
    UUID id,
    Integer turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer wordCount,
    Integer durationSeconds,
    Double asrConfidence,
    String pronunciationOverall,
    String wordFeedback
) {
}
