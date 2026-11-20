package com.sep.vox.domain.mapper;

import java.time.Instant;

import com.sep.vox.domain.dto.AssessmentSecurePoolDto;
import com.sep.vox.domain.model.assessment.AssessmentSecurePool;

public final class AssessmentSecurePoolDtoMapper {

    private AssessmentSecurePoolDtoMapper() {
    }

    public static AssessmentSecurePoolDto toDto(AssessmentSecurePool domain) {
        return new AssessmentSecurePoolDto(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getStatus().name(),
            domain.getReleaseMode().name(),
            valueOf(domain.getEmbargoUntil()),
            valueOf(domain.getReleasedAt()),
            domain.getReleasedBy()
        );
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
