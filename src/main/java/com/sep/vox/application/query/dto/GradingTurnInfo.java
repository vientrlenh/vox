package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * One question-answer turn in a part of the assessment. A part has MANY turns, each with its own audio.
 *
 * <p>{@code pronunciationOverall}/{@code wordFeedback} keep the JSON string as it was
 * stored, not parsed in the BE -- the same convention as {@code AssessmentItemEvaluationTurnResponse} so the
 * FE can use one shared parser for both screens.
 */
public record GradingTurnInfo(
    UUID id,
    Integer turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    Double asrConfidence,
    String pronunciationOverall,
    String wordFeedback
) {
}
