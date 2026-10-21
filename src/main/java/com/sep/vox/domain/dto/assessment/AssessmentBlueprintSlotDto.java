package com.sep.vox.domain.dto.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

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
    QuestionSelectionSpec selectionSpec,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {

    public static AssessmentBlueprintSlotDto toDto(AssessmentBlueprintSlot assessmentBlueprintSlot) {
        return new AssessmentBlueprintSlotDto(
            assessmentBlueprintSlot.getId(),
            assessmentBlueprintSlot.getSectionId(),
            assessmentBlueprintSlot.getBlueprintVersionId(),
            assessmentBlueprintSlot.getOrder(),
            assessmentBlueprintSlot.getWeight(),
            assessmentBlueprintSlot.getPrepTimeSecondsOverride(),
            assessmentBlueprintSlot.getResponseTimeSecondsOverride(),
            assessmentBlueprintSlot.getSlotType(),
            assessmentBlueprintSlot.getFixedQuestionId(),
            assessmentBlueprintSlot.getSelectionSpec(),
            assessmentBlueprintSlot.getCreatedAt(),
            assessmentBlueprintSlot.getUpdatedAt(),
            assessmentBlueprintSlot.getCreatedBy(),
            assessmentBlueprintSlot.getUpdatedBy()
        );
    }

    public static List<AssessmentBlueprintSlotDto> toDtoList(List<AssessmentBlueprintSlot> domains) {
        return domains.stream()
            .map(AssessmentBlueprintSlotDto::toDto)
            .toList();
    }
}
