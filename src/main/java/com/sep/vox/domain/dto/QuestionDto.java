package com.sep.vox.domain.dto;

import java.time.Instant;
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
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
