package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolStatus;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class SchoolMapper {

    private SchoolMapper() {}
    
    public static School toDomain(SchoolJpaEntity jpa) {
        try {
            return School.builder()
                .id(jpa.getId())
                .code(Code.from(jpa.getCode()))
                .name(Name.from(jpa.getName()))
                .domain(SchoolDomain.from(jpa.getDomain()))
                .wardId(jpa.getWardId())
                .cityId(jpa.getCityId())
                .provinceId(jpa.getProvinceId())
                .streetAddress(jpa.getStreetAddress())
                .studentCount(PositiveInteger.from(jpa.getStudentCount()))
                .status(SchoolStatus.from(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .registeredBy(jpa.getRegisteredBy())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school entity to school model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }

    public static SchoolJpaEntity toJpa(School school) {
        try {
            return SchoolJpaEntity.builder()
                .id(school.getId())
                .code(Code.valueOf(school.getCode()))
                .name(Name.valueOf(school.getName()))
                .description(school.getDescription())
                .domain(SchoolDomain.valueOf(school.getDomain()))
                .wardId(school.getWardId())
                .cityId(school.getCityId())
                .provinceId(school.getProvinceId())
                .streetAddress(school.getStreetAddress())
                .studentCount(PositiveInteger.valueOf(school.getStudentCount()))
                .status(SchoolStatus.value(school.getStatus()))
                .createdAt(school.getCreatedAt())
                .updatedAt(school.getUpdatedAt())
                .registeredBy(school.getRegisteredBy())
                .createdBy(school.getCreatedBy())
                .updatedBy(school.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school model to school entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }

    }
}
