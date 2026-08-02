package com.sep.vox.domain.model.personalization;

import java.util.List;
import java.util.UUID;

public record SubmitPracticeTurn(
    UUID sessionId,
    UUID questionId,
    int turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    int durationSeconds,
    String wordFeedbackJson,
    Double turnScore,
    boolean questionComplete,
    List<TurnCorrectionSubmission> corrections
) {
}
