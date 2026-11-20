package com.sep.vox.application.response.input.exam;

import java.util.UUID;

import com.sep.vox.domain.dto.QuestionAssetDto;

public record StudentQuestionResponse(
    UUID id,
    String code,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    int preparationTimeSeconds,
    int minResponseSeconds,
    int maxResponseSeconds,
    String type,
    String difficultyLevel,
    QuestionAssetDto asset
) {
}
