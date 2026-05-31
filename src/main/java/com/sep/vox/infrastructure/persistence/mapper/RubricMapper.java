package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.infrastructure.persistence.entity.RubricJpaEntity;

public final class RubricMapper {

    private RubricMapper() {}

    public static Rubric toDomain(RubricJpaEntity jpa) {
        return new Rubric(
            jpa.getId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getLanguageId(),
            jpa.getFrameworkId(),
            fromOwnerType(jpa.getOwnerType()),
            jpa.getSchoolId(),
            jpa.getCurrentVersionId()
        );
    }

    public static RubricJpaEntity toJpa(Rubric rubric) {
        return new RubricJpaEntity(
            rubric.getId(),
            rubric.getCode(),
            rubric.getName(),
            rubric.getDescription(),
            rubric.getLanguageId(),
            rubric.getFrameworkId(),
            valueOf(rubric.getOwnerType()),
            rubric.getSchoolId(),
            rubric.getCurrentVersionId()
        );
    }

    private static String valueOf(RubricOwnerType type) {
        return type == null ? null : type.name();
    }

    private static RubricOwnerType fromOwnerType(String value) {
        return value == null ? null : RubricOwnerType.valueOf(value);
    }
}
