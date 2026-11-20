package com.sep.vox.application.port.input.command.assessmentevaluation;

public record PronunciationOverallInput(
    Double accuracyScore,
    Double fluencyScore,
    Double prosodyScore,
    Double pronScore,
    Double completenessScore
) {
}
