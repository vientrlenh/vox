package com.sep.vox.domain.mapper;


import com.sep.vox.domain.dto.AssessmentSecurePoolDto;
import com.sep.vox.domain.model.assessment.AssessmentSecurePool;

public final class AssessmentSecurePoolDtoMapper {

    private AssessmentSecurePoolDtoMapper() {
    }

    public static AssessmentSecurePoolDto toDto(AssessmentSecurePool domain) {
        return new AssessmentSecurePoolDto(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getStatus(),
            domain.getReleaseMode(),
            domain.getEmbargoUntil(),
            domain.getReleasedAt(),
            domain.getReleasedBy()
        );
    }
}
