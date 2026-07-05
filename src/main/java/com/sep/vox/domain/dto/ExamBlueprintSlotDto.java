package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamBlueprintSlotDto(
    UUID id,
    UUID sectionId,
    UUID blueprintVersionId,
    int order,
    BigDecimal weight,
    Integer prepTimeSecondsOverride,
    Integer responseTimeSecondsOverride,
    String slotType,
    UUID fixedQuestionId,
    QuestionSelectionSpecDto selectionSpec,
    String createdAt,
    String updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
