package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassJpaEntity;

public final class SchoolClassMapper {

    public static SchoolClass toDomain(SchoolClassJpaEntity jpa) {
        return SchoolClass.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .languageId(jpa.getLanguageId())
            .schoolGradeId(jpa.getSchoolGradeId())
            .code(new ClassCode(jpa.getCode()))
            .name(jpa.getName())
            .description(jpa.getDescription())
            .status(statusFromString(jpa.getStatus()))
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static SchoolClassJpaEntity toJpa(SchoolClass schoolClass) {
        return SchoolClassJpaEntity.builder()
            .id(schoolClass.getId())
            .schoolId(schoolClass.getSchoolId())
            .languageId(schoolClass.getLanguageId())
            .schoolGradeId(schoolClass.getSchoolGradeId())
            .code(valueOf(schoolClass.getCode()))
            .name(schoolClass.getName())
            .description(schoolClass.getDescription())
            .status(valueOf(schoolClass.getStatus()))
            .createdAt(schoolClass.getCreatedAt())
            .updatedAt(schoolClass.getUpdatedAt())
            .createdBy(schoolClass.getCreatedBy())
            .updatedBy(schoolClass.getUpdatedBy())
            .build();
    }

    private static String valueOf(ClassCode code) {
        return code == null ? null : code.value();
    }

    private static String valueOf(SchoolClassStatus status) {
        return status == null ? null : status.name();
    }

    private static SchoolClassStatus statusFromString(String status) {
        return status == null ? null : SchoolClassStatus.valueOf(status);
    }
}
