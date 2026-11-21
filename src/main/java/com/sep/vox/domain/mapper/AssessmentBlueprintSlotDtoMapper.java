package com.sep.vox.domain.mapper;

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
            domain.getSlotType(),
            domain.getFixedQuestionId(),
            QuestionSelectionSpecDtoMapper.toDto(domain.getSelectionSpec()),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.createdByUserId().orElse(null),
            domain.updatedByUserId().orElse(null)
        );
    }

    public static List<AssessmentBlueprintSlotDto> toDtoList(List<AssessmentBlueprintSlot> domains) {
        return domains.stream()
            .map(AssessmentBlueprintSlotDtoMapper::toDto)
            .toList();
    }
}
