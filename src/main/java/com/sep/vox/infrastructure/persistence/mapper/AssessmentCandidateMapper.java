package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateJpaEntity;

public final class AssessmentCandidateMapper {

    private AssessmentCandidateMapper() {}

    public static AssessmentCandidate toDomain(AssessmentCandidateJpaEntity jpa) {
        return AssessmentCandidate.builder()
                .id(jpa.getId())
                .assessmentId(jpa.getAssessmentId())
                .studentId(jpa.getStudentId())
                .assignedPaperId(jpa.getAssignedPaperId())
                .scheduleId(jpa.getScheduleId())
                .status(statusFromString(jpa.getStatus()))
                .assignedAt(jpa.getAssignedAt())
                .updatedAt(jpa.getUpdatedAt())
                .blockedAt(jpa.getBlockedAt())
                .assignedBy(jpa.getAssignedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentCandidateJpaEntity toJpa(AssessmentCandidate domain) {
        return AssessmentCandidateJpaEntity.builder()
                .id(domain.getId())
                .assessmentId(domain.getAssessmentId())
                .studentId(domain.getStudentId())
                .assignedPaperId(domain.getAssignedPaperId())
                .scheduleId(domain.getScheduleId())
                .status(domain.getStatus().name())
                .assignedAt(domain.getAssignedAt())
                .updatedAt(domain.getUpdatedAt())
                .blockedAt(domain.getBlockedAt())
                .assignedBy(domain.getAssignedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    private static AssessmentCandidateStatus statusFromString(String status) {
        return status == null ? null : AssessmentCandidateStatus.valueOf(status);
    }
}
