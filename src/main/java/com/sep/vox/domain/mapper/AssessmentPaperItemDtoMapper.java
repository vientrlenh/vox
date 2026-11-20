package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.AssessmentPaperItemDto;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;

public final class AssessmentPaperItemDtoMapper {

    private AssessmentPaperItemDtoMapper() {
    }

    public static AssessmentPaperItemDto toDto(AssessmentPaperItem domain) {
        return new AssessmentPaperItemDto(
            domain.getId(),
            domain.getBlueprintSlotId(),
            domain.getSectionId(),
            domain.getPaperId(),
            domain.getQuestionId(),
            domain.getOrder(),
            domain.getWeight()
        );
    }

    public static List<AssessmentPaperItemDto> toDtoList(List<AssessmentPaperItem> domains) {
        return domains.stream()
            .map(AssessmentPaperItemDtoMapper::toDto)
            .toList();
    }
}
