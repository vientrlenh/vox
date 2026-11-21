package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolGradeJpaEntity;

public final class SchoolGradeMapper {
    
    public static final SchoolGrade toDomain(SchoolGradeJpaEntity jpa) {
        return SchoolGrade.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .gradeLevelId(jpa.getGradeLevelId())
            .code(jpa.getCode())
            .name(jpa.getName())
            .description(jpa.getDescription())
            .startDate(jpa.getStartDate())
            .endDate(jpa.getEndDate())
            .status(statusFromString(jpa.getStatus()))
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static final SchoolGradeJpaEntity toJpa(SchoolGrade grade) {
        return SchoolGradeJpaEntity.builder()
            .id(grade.getId())
            .schoolId(grade.getSchoolId())
            .gradeLevelId(grade.getGradeLevelId())
            .code(grade.getCode())
            .name(grade.getName())
            .description(grade.getDescription())
            .startDate(grade.getStartDate())
            .endDate(grade.getEndDate())
            .status(valueOf(grade.getStatus()))
            .createdAt(grade.getCreatedAt())
            .updatedAt(grade.getUpdatedAt())
            .createdBy(grade.getCreatedBy())
            .updatedBy(grade.getUpdatedBy())
            .build();
    }

    private static SchoolGradeStatus statusFromString(String status) {
        return status == null ? null : SchoolGradeStatus.valueOf(status);
    }

    private static String valueOf(SchoolGradeStatus status) {
        return status == null ? null : status.name();
    }
}
