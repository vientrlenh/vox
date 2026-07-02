package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamSecurePool;
import com.sep.vox.domain.model.exam.ExamSecurePoolReleaseMode;
import com.sep.vox.domain.model.exam.ExamSecurePoolStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamSecurePoolJpaEntity;

public final class ExamSecurePoolMapper {

    private ExamSecurePoolMapper() {}

    public static ExamSecurePool toDomain(ExamSecurePoolJpaEntity jpa) {
        return new ExamSecurePool(
            jpa.getId(),
            jpa.getExamId(),
            statusFromString(jpa.getStatus()),
            releaseModeFromString(jpa.getReleaseMode()),
            jpa.getEmbargoUntil(),
            jpa.getReleasedAt(),
            jpa.getReleasedBy(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ExamSecurePoolJpaEntity toJpa(ExamSecurePool domain) {
        return new ExamSecurePoolJpaEntity(
            domain.getId(),
            domain.getExamId(),
            domain.getStatus().name(),
            domain.getReleaseMode().name(),
            domain.getEmbargoUntil(),
            domain.getReleasedAt(),
            domain.getReleasedBy(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    private static ExamSecurePoolStatus statusFromString(String status) {
        return status == null ? null : ExamSecurePoolStatus.valueOf(status);
    }

    private static ExamSecurePoolReleaseMode releaseModeFromString(String releaseMode) {
        return releaseMode == null ? null : ExamSecurePoolReleaseMode.valueOf(releaseMode);
    }
}
