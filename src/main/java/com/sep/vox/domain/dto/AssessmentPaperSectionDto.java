package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentPaperSection;

public record AssessmentPaperSectionDto(
    UUID id,
    UUID paperId,
    int order,
    String title,
    String instruction,
    BigDecimal weight,
    Integer sectionTimeLimitSeconds
) {

    public static AssessmentPaperSectionDto toDto(AssessmentPaperSection assessmentPaperSection) {
        return new AssessmentPaperSectionDto(
            assessmentPaperSection.getId(),
            assessmentPaperSection.getPaperId(),
            assessmentPaperSection.getOrder(),
            assessmentPaperSection.getTitle(),
            assessmentPaperSection.getInstruction(),
            assessmentPaperSection.getWeight(),
            assessmentPaperSection.getSectionTimeLimitSeconds()
        );
    }

    public static List<AssessmentPaperSectionDto> toDtoList(List<AssessmentPaperSection> domains) {
        return domains.stream()
            .map(AssessmentPaperSectionDto::toDto)
            .toList();
    }
}
