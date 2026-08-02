package com.sep.vox.domain.dto.personalization;

public record TurnCorrectionDto(
    String category,
    String originalText,
    String correctedText,
    String explanation,
    String correctAudioUrl
) {
}
