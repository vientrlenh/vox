package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.language.LearningLanguage;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class SchoolClassMapper {

    public static SchoolClass toDomain(SchoolClassJpaEntity jpa) {
        try {
            return SchoolClass.builder()
                .id(jpa.getId())
                .schoolId(jpa.getSchoolId())
                .language(LearningLanguage.from(jpa.getLanguage()))
                .schoolGradeId(jpa.getSchoolGradeId())
                .code(Code.from(jpa.getCode()))
                .name(Name.from(jpa.getName()))
                .description(jpa.getDescription())
                .status(SchoolClassStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .archivedAt(jpa.getArchivedAt())
                .createdBy(jpa.getCreatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school class entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }

    public static SchoolClassJpaEntity toJpa(SchoolClass sClass) {
        try {
            return SchoolClassJpaEntity.builder()
                .id(sClass.getId())
                .schoolId(sClass.getSchoolId())
                .language(LearningLanguage.value(sClass.getLanguage()))
                .schoolGradeId(sClass.getSchoolGradeId())
                .code(Code.valueOf(sClass.getCode()))
                .name(Name.valueOf(sClass.getName()))
                .description(sClass.getDescription())
                .status(SchoolClassStatus.value(sClass.getStatus()))
                .createdAt(sClass.getCreatedAt())
                .updatedAt(sClass.getUpdatedAt())
                .archivedAt(sClass.getArchivedAt())
                .createdBy(sClass.getCreatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school class model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A probem occurred when converting model to entity");
        }

    }
}
