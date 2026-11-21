package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricJpaEntity;

public final class RubricMapper {

    private RubricMapper() {}

    public static Rubric toDomain(RubricJpaEntity jpa) {
        return Rubric.builder()
                .id(jpa.getId())
                .languageId(jpa.getLanguageId())
                .frameworkId(jpa.getFrameworkId())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .ownerType(fromOwnerType(jpa.getOwnerType()))
                .schoolId(jpa.getSchoolId())
                .build();
    }

    public static RubricJpaEntity toJpa(Rubric rubric) {
        return RubricJpaEntity.builder()
                .id(rubric.getId())
                .languageId(rubric.getLanguageId())
                .frameworkId(rubric.getFrameworkId())
                .code(rubric.getCode())
                .name(rubric.getName())
                .description(rubric.getDescription())
                .ownerType(valueOf(rubric.getOwnerType()))
                .schoolId(rubric.getSchoolId())
                .build();
    }

    private static String valueOf(RubricOwnerType type) {
        return type == null ? null : type.name();
    }

    private static RubricOwnerType fromOwnerType(String value) {
        return value == null ? null : RubricOwnerType.valueOf(value);
    }
}
