package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;

public final class ExamSessionMapper {

    private ExamSessionMapper() {}

    public static ExamSession toDomain(ExamSessionJpaEntity jpa) {
        return new ExamSession(
            jpa.getId(),
            jpa.getExamId(),
            jpa.getCandidateId(),
            jpa.getPaperId(),
            jpa.getStartedAt(),
            jpa.getSubmittedAt(),
            statusFromString(jpa.getStatus()),
            jpa.isFlagged(),
            jpa.getFlagReason()
        );
    }

    public static ExamSessionJpaEntity toJpa(ExamSession domain) {
        return new ExamSessionJpaEntity(
            domain.getId(),
            domain.getExamId(),
            domain.getCandidateId(),
            domain.getPaperId(),
            domain.getStartedAt(),
            domain.getSubmittedAt(),
            domain.getStatus().name(),
            domain.isFlagged(),
            domain.getFlagReason()
        );
    }

    private static ExamSessionStatus statusFromString(String status) {
        return status == null ? null : ExamSessionStatus.valueOf(status);
    }
}
