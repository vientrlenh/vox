package com.sep.vox.interfaces.rest.dto.request.assessment;

public record UpdateAssessmentItemAnswerTurnRequest(
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    String answeredAt
) {
}
