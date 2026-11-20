package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;

public final class AssessmentCandidateDtoMapper {

    private AssessmentCandidateDtoMapper() {
    }

    public static AssessmentCandidateDto toDto(AssessmentCandidate domain) {
        return new AssessmentCandidateDto(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getStudentId(),
            domain.getAssignedPaperId(),
            domain.getScheduleId(),
            domain.getStatus() == null ? null : domain.getStatus().name(),
            valueOf(domain.getAssignedAt()),
            valueOf(domain.getUpdatedAt()),
            valueOf(domain.getBlockedAt())
        );
    }

    public static List<AssessmentCandidateDto> toDtoList(List<AssessmentCandidate> domains) {
        return domains.stream()
            .map(AssessmentCandidateDtoMapper::toDto)
            .toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
