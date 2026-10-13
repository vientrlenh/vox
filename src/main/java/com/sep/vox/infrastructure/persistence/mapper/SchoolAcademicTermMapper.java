package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolAcademicTerm;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolAcademicTermJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SchoolAcademicTermMapper {

    private SchoolAcademicTermMapper() {}

    public static SchoolAcademicTerm toDomain(SchoolAcademicTermJpaEntity jpa) {
        try {
            return SchoolAcademicTerm.builder()
                .id(jpa.getId())
                .academicYearId(jpa.getAcademicYearId())
                .name(jpa.getName())
                .order(PositiveInteger.from(jpa.getOrder()))
                .startDate(jpa.getStartDate())
                .endDate(jpa.getEndDate())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school academic term entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static SchoolAcademicTermJpaEntity toJpa(SchoolAcademicTerm term) {
        try {
            return SchoolAcademicTermJpaEntity.builder()
                .id(term.getId())
                .academicYearId(term.getAcademicYearId())
                .name(term.getName())
                .order(PositiveInteger.valueOf(term.getOrder()))
                .startDate(term.getStartDate())
                .endDate(term.getEndDate())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school academic term model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
