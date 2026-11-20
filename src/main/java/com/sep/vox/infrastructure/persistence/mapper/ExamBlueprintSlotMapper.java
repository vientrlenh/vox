package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;
import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintSlotJpaEntity;

public final class ExamBlueprintSlotMapper {

    private ExamBlueprintSlotMapper() {
    }

    public static ExamBlueprintSlot toDomain(ExamBlueprintSlotJpaEntity jpa) {
        return new ExamBlueprintSlot(
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

    public static ExamBlueprintSlotJpaEntity toJpa(ExamBlueprintSlot domain) {
        return new ExamBlueprintSlotJpaEntity(
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

    private static ExamBlueprintSlotType slotTypeFromString(String value) {
        return value == null ? null : ExamBlueprintSlotType.valueOf(value);
    }

    private static QuestionSelectionSpec selectionSpecFromJson(String value) {
        return value == null ? null : JsonValueObjectMapper.fromJson(value, QuestionSelectionSpec.class);
    }

    private static String selectionSpecToJson(QuestionSelectionSpec value) {
        return value == null ? null : JsonValueObjectMapper.toJson(value);
    }
}
