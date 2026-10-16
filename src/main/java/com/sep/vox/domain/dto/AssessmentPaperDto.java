package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;

public record AssessmentPaperDto(
    UUID id,
    UUID assessmentId,
    UUID blueprintVersionId,
    String code,
    int variant,
    AssessmentPaperStatus status,
    Integer timeDurationSeconds,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {

    public static AssessmentPaperDto toDto(AssessmentPaper assessmentPaper) {
        return new AssessmentPaperDto(
            assessmentPaper.getId(),
            assessmentPaper.getAssessmentId(),
            assessmentPaper.getBlueprintVersionId(),
            assessmentPaper.getCode(),
            assessmentPaper.getVariant(),
            assessmentPaper.getStatus(),
            assessmentPaper.getTimeDurationSeconds(),
            assessmentPaper.getCreatedAt(),
            assessmentPaper.getUpdatedAt(),
            assessmentPaper.getCreatedBy(),
            assessmentPaper.getUpdatedBy()
        );
    }

    public static List<AssessmentPaperDto> toDtoList(List<AssessmentPaper> domains) {
        return domains.stream()
            .map(AssessmentPaperDto::toDto)
            .toList();
    }
}
