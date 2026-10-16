package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintSlotJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.JsonValueObjectMapper;

public final class AssessmentBlueprintSlotMapper {

    private AssessmentBlueprintSlotMapper() {
    }

    public static AssessmentBlueprintSlot toDomain(AssessmentBlueprintSlotJpaEntity jpa) {
        return new AssessmentBlueprintSlot(
            jpa.getId(),
            jpa.getSectionId(),
            jpa.getBlueprintVersionId(),
            jpa.getOrder(),
            jpa.getWeight(),
            jpa.getPrepTimeSecondsOverride(),
            jpa.getResponseTimeSecondsOverride(),
            slotTypeFromString(jpa.getSlotType()),
            jpa.getFixedQuestionId(),
            selectionSpecFromJson(jpa.getSelectionSpec()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentBlueprintSlotJpaEntity toJpa(AssessmentBlueprintSlot domain) {
        return new AssessmentBlueprintSlotJpaEntity(
            domain.getId(),
            domain.getSectionId(),
            domain.getBlueprintVersionId(),
            domain.getOrder(),
            domain.getWeight(),
            domain.getPrepTimeSecondsOverride(),
            domain.getResponseTimeSecondsOverride(),
            domain.getSlotType().name(),
            domain.getFixedQuestionId(),
            selectionSpecToJson(domain.getSelectionSpec()),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
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
