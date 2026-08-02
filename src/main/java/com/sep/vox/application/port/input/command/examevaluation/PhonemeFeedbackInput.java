package com.sep.vox.application.port.input.command.examevaluation;

public record PhonemeFeedbackInput(
    String phoneme,
    Double accuracyScore
) {
}
