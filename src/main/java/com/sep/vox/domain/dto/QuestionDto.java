package com.sep.vox.domain.dto;

import java.util.UUID;

public record QuestionDto(
    UUID id,
    UUID questionBankId,
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
    String sharing,
    UUID sourceQuestionId,
    boolean locked,
    String status,
    String confidentiality,
    UUID securePoolId,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
