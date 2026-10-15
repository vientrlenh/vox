package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolDirectoryJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SchoolDirectoryMapper {

    private SchoolDirectoryMapper() {}

    public static SchoolDirectory toDomain(SchoolDirectoryJpaEntity jpa) {
        try {
            return SchoolDirectory.builder()
                .id(jpa.getId())
                .code(Code.from(jpa.getCode()))
                .name(jpa.getName())
                .wardId(jpa.getWardId())
                .cityId(jpa.getCityId())
                .provinceId(jpa.getProvinceId())
                .domain(SchoolDomain.from(jpa.getDomain()))
                .streetAddress(jpa.getStreetAddress())
                .origin(SchoolDirectoryOrigin.from(jpa.getOrigin()))
                .verified(jpa.isVerified())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school directory entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static SchoolDirectoryJpaEntity toJpa(SchoolDirectory directory) {
        try {
            return SchoolDirectoryJpaEntity.builder()
                .id(directory.getId())
                .code(Code.valueOf(directory.getCode()))
                .name(directory.getName())
                .wardId(directory.getWardId())
                .cityId(directory.getCityId())
                .provinceId(directory.getProvinceId())
                .domain(SchoolDomain.valueOf(directory.getDomain()))
                .streetAddress(directory.getStreetAddress())
                .origin(SchoolDirectoryOrigin.value(directory.getOrigin()))
                .verified(directory.isVerified())
                .createdAt(directory.getCreatedAt())
                .updatedAt(directory.getUpdatedAt())
                .createdBy(directory.getCreatedBy())
                .updatedBy(directory.getUpdatedBy())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping school directory model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
