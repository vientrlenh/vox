package com.sep.vox.domain.mapper;

import java.util.List;

import com.sep.vox.domain.dto.AssessmentPaperSectionDto;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;

public final class AssessmentPaperSectionDtoMapper {

    private AssessmentPaperSectionDtoMapper() {
    }

    public static AssessmentPaperSectionDto toDto(AssessmentPaperSection domain) {
        return new AssessmentPaperSectionDto(
            domain.getId(),
            domain.getPaperId(),
            domain.getOrder(),
            domain.getTitle(),
            domain.getInstruction(),
            domain.getWeight(),
            domain.getSectionTimeLimitSeconds()
        );
    }

    public static List<AssessmentPaperSectionDto> toDtoList(List<AssessmentPaperSection> domains) {
        return domains.stream()
            .map(AssessmentPaperSectionDtoMapper::toDto)
            .toList();
    }
}
