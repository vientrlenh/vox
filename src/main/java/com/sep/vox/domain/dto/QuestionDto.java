package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionDto(
    UUID id,
    UUID questionTopicId,
    String code,
    String instructionText,
    String questionText,
    String promptText,
    String preparationText,
    String type,
    int preparationTimeSeconds,
    int minResponseSeconds,
    int maxResponseSeconds,
    String scope,
    String visibility,
    UUID sourceQuestionId,
    boolean locked,
    String status,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
