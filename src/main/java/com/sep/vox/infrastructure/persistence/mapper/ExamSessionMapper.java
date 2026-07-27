package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;

public final class ExamSessionMapper {

    private ExamSessionMapper() {}

    public static ExamSession toDomain(ExamSessionJpaEntity jpa) {
        var domain = new ExamSession(
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
        domain.setChosenStreamType(chosenStreamTypeFromString(jpa.getChosenStreamType()));
        return domain;
    }

    public static ExamSessionJpaEntity toJpa(ExamSession domain) {
        var jpa = new ExamSessionJpaEntity(
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
        jpa.setChosenStreamType(
            domain.getChosenStreamType() == null ? null : domain.getChosenStreamType().name()
        );
        return jpa;
    }

    private static ExamSessionStatus statusFromString(String status) {
        return status == null ? null : ExamSessionStatus.valueOf(status);
    }

    private static ExamRequiredStreamType chosenStreamTypeFromString(String chosenStreamType) {
        return chosenStreamType == null ? null : ExamRequiredStreamType.valueOf(chosenStreamType);
    }
}
