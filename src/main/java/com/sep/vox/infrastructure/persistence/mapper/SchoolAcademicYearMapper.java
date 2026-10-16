package com.sep.vox.infrastructure.persistence.mapper;


import com.sep.vox.domain.model.school.SchoolAcademicYear;
import com.sep.vox.domain.model.school.SchoolAcademicYearStatus;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolAcademicYearJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SchoolAcademicYearMapper {

    private SchoolAcademicYearMapper() {}

    public static SchoolAcademicYear toDomain(SchoolAcademicYearJpaEntity jpa) {
        try {
            return SchoolAcademicYear.builder()
                .id(jpa.getId())
                .schoolId(jpa.getSchoolId())
                .name(jpa.getName())
                .startDate(jpa.getStartDate())
                .endDate(jpa.getEndDate())
                .status(SchoolAcademicYearStatus.from(jpa.getStatus()))
                .activatedAt(jpa.getActivatedAt())
                .closedAt(jpa.getClosedAt())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school academic year entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static SchoolAcademicYearJpaEntity toJpa(SchoolAcademicYear year) {
        try {
            return SchoolAcademicYearJpaEntity.builder()
                .id(year.getId())
                .schoolId(year.getSchoolId())
                .name(year.getName())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .status(SchoolAcademicYearStatus.value(year.getStatus()))
                .activatedAt(year.getActivatedAt())
                .closedAt(year.getClosedAt())
                .createdAt(year.getCreatedAt())
                .updatedAt(year.getUpdatedAt())
                .createdBy(year.getCreatedBy())
                .updatedBy(year.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school academic year model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
