package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;

public final class ExamCandidateMapper {

    private ExamCandidateMapper() {}

    public static ExamCandidate toDomain(ExamCandidateJpaEntity jpa) {
        return new ExamCandidate(
            jpa.getId(),
            jpa.getExamId(),
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

    public static ExamCandidateJpaEntity toJpa(ExamCandidate domain) {
        return new ExamCandidateJpaEntity(
            domain.getId(),
            domain.getExamId(),
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

    private static ExamCandidateStatus statusFromString(String status) {
        return status == null ? null : ExamCandidateStatus.valueOf(status);
    }
}
