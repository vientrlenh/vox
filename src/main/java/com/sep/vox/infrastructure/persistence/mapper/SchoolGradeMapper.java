package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolGradeJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class SchoolGradeMapper {

    private SchoolGradeMapper() {}
    
    public static SchoolGrade toDomain(SchoolGradeJpaEntity jpa) {
        try {
            return SchoolGrade.builder()
                .id(jpa.getId())
                .schoolId(jpa.getSchoolId())
                .gradeLevelId(jpa.getGradeLevelId())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .academicTimelineId(jpa.getAcademicTimelineId())
                .status(SchoolGradeStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("A problem occurred when mapping school grade entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("An error occurred when converting entity to model");
        }
        
    }

    public static SchoolGradeJpaEntity toJpa(SchoolGrade grade) {
        try {
            return SchoolGradeJpaEntity.builder()
                .id(grade.getId())
                .schoolId(grade.getSchoolId())
                .gradeLevelId(grade.getGradeLevelId())
                .name(grade.getName())
                .description(grade.getDescription())
                .academicTimelineId(grade.getAcademicTimelineId())
                .status(SchoolGradeStatus.value(grade.getStatus()))
                .createdAt(grade.getCreatedAt())
                .updatedAt(grade.getUpdatedAt())
                .createdBy(grade.getCreatedBy())
                .updatedBy(grade.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("A problem occurred when mapping school grade model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("An error occurred when converting model to entity");
        }
    }
}
