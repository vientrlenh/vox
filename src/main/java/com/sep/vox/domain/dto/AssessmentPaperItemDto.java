package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentPaperItem;

public record AssessmentPaperItemDto(
    UUID id,
    UUID blueprintSlotId,
    UUID sectionId,
    UUID paperId,
    UUID questionId,
    int order,
    BigDecimal weight
) {

    public static AssessmentPaperItemDto toDto(AssessmentPaperItem assessmentPaperItem) {
        return new AssessmentPaperItemDto(
            assessmentPaperItem.getId(),
            assessmentPaperItem.getBlueprintSlotId(),
            assessmentPaperItem.getSectionId(),
            assessmentPaperItem.getPaperId(),
            assessmentPaperItem.getQuestionId(),
            assessmentPaperItem.getOrder(),
            assessmentPaperItem.getWeight()
        );
    }

    public static List<AssessmentPaperItemDto> toDtoList(List<AssessmentPaperItem> domains) {
        return domains.stream()
            .map(AssessmentPaperItemDto::toDto)
            .toList();
    }
}
