package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateJpaEntity;

public final class AssessmentCandidateMapper {

    private AssessmentCandidateMapper() {}

    public static AssessmentCandidate toDomain(AssessmentCandidateJpaEntity jpa) {
        return new AssessmentCandidate(
            jpa.getId(),
            jpa.getAssessmentId(),
            jpa.getStudentId(),
            jpa.getAssignedPaperId(),
            jpa.getScheduleId(),
            statusFromString(jpa.getStatus()),
            jpa.getAssignedAt(),
            jpa.getUpdatedAt(),
            jpa.getBlockedAt(),
            jpa.getAssignedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentCandidateJpaEntity toJpa(AssessmentCandidate domain) {
        return new AssessmentCandidateJpaEntity(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getStudentId(),
            domain.getAssignedPaperId(),
            domain.getScheduleId(),
            domain.getStatus().name(),
            domain.getAssignedAt(),
            domain.getUpdatedAt(),
            domain.getBlockedAt(),
            domain.getAssignedBy(),
            domain.getUpdatedBy()
        );
    }

    private static AssessmentCandidateStatus statusFromString(String status) {
        return status == null ? null : AssessmentCandidateStatus.valueOf(status);
    }
}
