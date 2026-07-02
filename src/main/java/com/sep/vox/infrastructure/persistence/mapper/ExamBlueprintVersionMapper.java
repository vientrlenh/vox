package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintVersionJpaEntity;

public final class ExamBlueprintVersionMapper {

    private ExamBlueprintVersionMapper() {
    }

    public static ExamBlueprintVersion toDomain(ExamBlueprintVersionJpaEntity jpa) {
        return new ExamBlueprintVersion(
            jpa.getId(),
            jpa.getBlueprintId(),
            jpa.getVersion(),
            jpa.getCode(),
            jpa.getDescription(),
            statusFromString(jpa.getStatus()),
            jpa.getTotalTimeLimitSeconds(),
            jpa.getEffectiveFrom(),
            jpa.getEffectiveTo(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ExamBlueprintVersionJpaEntity toJpa(ExamBlueprintVersion domain) {
        return new ExamBlueprintVersionJpaEntity(
            domain.getId(),
            domain.getBlueprintId(),
            domain.getVersion(),
            domain.getCode(),
            domain.getDescription(),
            domain.getStatus().name(),
            domain.getTotalTimeLimitSeconds(),
            domain.getEffectiveFrom(),
            domain.getEffectiveTo(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }

    private static ExamBlueprintVersionStatus statusFromString(String value) {
        return value == null ? null : ExamBlueprintVersionStatus.valueOf(value);
    }
}
