package com.sep.vox.application.port.input.command.examevaluation;

import java.util.List;

public record TurnDetailInput(
    String turnId,
    Integer turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer wordCount,
    Integer durationSeconds,
    Double asrConfidence,
    PronunciationOverallInput pronunciationOverall,
    List<WordFeedbackInput> wordFeedback
) {
}
