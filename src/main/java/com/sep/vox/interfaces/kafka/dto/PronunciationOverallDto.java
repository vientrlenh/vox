package com.sep.vox.interfaces.kafka.dto;

public record PronunciationOverallDto(
    Double accuracyScore,
    Double fluencyScore,
    Double prosodyScore,
    Double pronScore,
    Double completenessScore
) {
}
