package com.sep.vox.application.port.input.command.examevaluation;

import java.util.List;

public record WordFeedbackInput(
    String word,
    Double accuracyScore,
    Boolean hasCriticalIssue,
    List<PhonemeFeedbackInput> phonemes
) {
}
