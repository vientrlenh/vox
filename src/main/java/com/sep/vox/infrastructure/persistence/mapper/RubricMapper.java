package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.infrastructure.persistence.entity.RubricJpaEntity;

public final class RubricMapper {

    private RubricMapper() {}

    public static Rubric toDomain(RubricJpaEntity jpa) {
        return new Rubric(
            jpa.getId(),
            jpa.getLanguageId(),
            jpa.getFrameworkId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            fromOwnerType(jpa.getOwnerType()),
            jpa.getSchoolId(),
            jpa.getCurrentVersionId()
        );
    }

    public static RubricJpaEntity toJpa(Rubric rubric) {
        return new RubricJpaEntity(
            rubric.getId(),
            rubric.getLanguageId(),
            rubric.getFrameworkId(),
            rubric.getCode(),
            rubric.getName(),
            rubric.getDescription(),
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
