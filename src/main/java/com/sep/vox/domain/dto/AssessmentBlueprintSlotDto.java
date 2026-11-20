package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

public record AssessmentBlueprintSlotDto(
    UUID id,
    UUID sectionId,
    UUID blueprintVersionId,
    int order,
    BigDecimal weight,
    Integer prepTimeSecondsOverride,
    Integer responseTimeSecondsOverride,
    AssessmentBlueprintSlotType slotType,
    UUID fixedQuestionId,
    QuestionSelectionSpecDto selectionSpec,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
}
