package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.model.assessment.AssessmentPaper;

public final class AssessmentPaperDtoMapper {

    private AssessmentPaperDtoMapper() {
    }

    public static AssessmentPaperDto toDto(AssessmentPaper domain) {
        return new AssessmentPaperDto(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getBlueprintVersionId(),
            domain.getCode(),
            domain.getVariant(),
            domain.getStatus().name(),
            domain.getTimeDurationSeconds(),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getUpdatedAt()),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    public static List<AssessmentPaperDto> toDtoList(List<AssessmentPaper> domains) {
        return domains.stream()
            .map(AssessmentPaperDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
