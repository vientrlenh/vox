package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintSlotJpaEntity;

public final class AssessmentBlueprintSlotMapper {

    private AssessmentBlueprintSlotMapper() {
    }

    public static AssessmentBlueprintSlot toDomain(AssessmentBlueprintSlotJpaEntity jpa) {
        return AssessmentBlueprintSlot.builder()
                .id(jpa.getId())
                .sectionId(jpa.getSectionId())
                .blueprintVersionId(jpa.getBlueprintVersionId())
                .order(jpa.getOrder())
                .weight(jpa.getWeight())
                .prepTimeSecondsOverride(jpa.getPrepTimeSecondsOverride())
                .responseTimeSecondsOverride(jpa.getResponseTimeSecondsOverride())
                .slotType(slotTypeFromString(jpa.getSlotType()))
                .fixedQuestionId(jpa.getFixedQuestionId())
                .selectionSpec(selectionSpecFromJson(jpa.getSelectionSpec()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentBlueprintSlotJpaEntity toJpa(AssessmentBlueprintSlot domain) {
        return AssessmentBlueprintSlotJpaEntity.builder()
                .id(domain.getId())
                .sectionId(domain.getSectionId())
                .blueprintVersionId(domain.getBlueprintVersionId())
                .order(domain.getOrder())
                .weight(domain.getWeight())
                .prepTimeSecondsOverride(domain.getPrepTimeSecondsOverride())
                .responseTimeSecondsOverride(domain.getResponseTimeSecondsOverride())
                .slotType(domain.getSlotType().name())
                .fixedQuestionId(domain.getFixedQuestionId())
                .selectionSpec(selectionSpecToJson(domain.getSelectionSpec()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private static AssessmentBlueprintSlotType slotTypeFromString(String value) {
        return value == null ? null : AssessmentBlueprintSlotType.valueOf(value);
    }

    private static QuestionSelectionSpec selectionSpecFromJson(String value) {
        return value == null ? null : JsonValueObjectMapper.fromJson(value, QuestionSelectionSpec.class);
    }

    private static String selectionSpecToJson(QuestionSelectionSpec value) {
        return value == null ? null : JsonValueObjectMapper.toJson(value);
    }
}
