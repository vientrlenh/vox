package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;

public final class SchoolGradeMapper {
    
    public static final SchoolGrade toDomain(SchoolGradeJpaEntity jpa) {
        return new SchoolGrade(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getGradeLevelId(),
            jpa.getCode(),
            jpa.getName(), 
            jpa.getDescription(), 
            jpa.getStartDate(), 
            jpa.getEndDate(), 
            statusFromString(jpa.getStatus()), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static final SchoolGradeJpaEntity toJpa(SchoolGrade grade) {
        return new SchoolGradeJpaEntity(
            grade.getId(),
            grade.getSchoolId(),
            grade.getGradeLevelId(),
            grade.getCode(),
            grade.getName(), 
            grade.getDescription(), 
            grade.getStartDate(), 
            grade.getEndDate(), 
            valueOf(grade.getStatus()), 
            grade.getCreatedAt(), 
            grade.getUpdatedAt(), 
            grade.getCreatedBy(), 
            grade.getUpdatedBy()
        );
    }

    private static SchoolGradeStatus statusFromString(String status) {
        return status == null ? null : SchoolGradeStatus.valueOf(status);
    }

    private static String valueOf(SchoolGradeStatus status) {
        return status == null ? null : status.name();
    }
}
