package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamPaperJpaEntity;

public final class ExamPaperMapper {

    private ExamPaperMapper() {}

    public static ExamPaper toDomain(ExamPaperJpaEntity jpa) {
        return new ExamPaper(
            jpa.getId(),
            jpa.getExamId(),
            jpa.getCode(),
            jpa.getVariant(),
            statusFromString(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ExamPaperJpaEntity toJpa(ExamPaper domain) {
        return new ExamPaperJpaEntity(
            domain.getId(),
            domain.getExamId(),
            domain.getCode(),
            domain.getVariant(),
            domain.getStatus().name(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    private static ExamPaperStatus statusFromString(String status) {
        return status == null ? null : ExamPaperStatus.valueOf(status);
    }
}
