package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;

public final class AssessmentBlueprintSlotDtoMapper {

    private AssessmentBlueprintSlotDtoMapper() {
    }

    public static AssessmentBlueprintSlotDto toDto(AssessmentBlueprintSlot domain) {
        return new AssessmentBlueprintSlotDto(
            domain.getId(),
            domain.getSectionId(),
            domain.getBlueprintVersionId(),
            domain.getOrder(),
            domain.getWeight(),
            domain.getPrepTimeSecondsOverride(),
            domain.getResponseTimeSecondsOverride(),
            domain.getSlotType().name(),
            domain.getFixedQuestionId(),
            QuestionSelectionSpecDtoMapper.toDto(domain.getSelectionSpec()),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<AssessmentBlueprintSlotDto> toDtoList(List<AssessmentBlueprintSlot> domains) {
        return domains.stream()
            .map(AssessmentBlueprintSlotDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
