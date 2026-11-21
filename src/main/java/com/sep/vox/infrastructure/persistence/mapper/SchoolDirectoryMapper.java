package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolDirectoryJpaEntity;

public final class SchoolDirectoryMapper {
    

    public static SchoolDirectory toDomain(SchoolDirectoryJpaEntity jpa) {
        return SchoolDirectory.builder()
            .id(jpa.getId())
            .code(jpa.getCode())
            .name(jpa.getName())
            .provinceCode(jpa.getProvinceCode())
            .provinceName(jpa.getProvinceName())
            .districtName(jpa.getDistrictName())
            .domain(jpa.getDomain())
            .address(jpa.getAddress())
            .origin(sourceFromString(jpa.getOrigin()))
            .verified(jpa.isVerified())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static SchoolDirectoryJpaEntity toJpa(SchoolDirectory sd) {
        return SchoolDirectoryJpaEntity.builder()
            .id(sd.getId())
            .code(sd.getCode())
            .name(sd.getName())
            .provinceCode(sd.getProvinceCode())
            .provinceName(sd.getProvinceName())
            .districtName(sd.getDistrictName())
            .domain(sd.getDomain())
            .address(sd.getAddress())
            .origin(valueOf(sd.getOrigin()))
            .verified(sd.isVerified())
            .createdAt(sd.getCreatedAt())
            .updatedAt(sd.getUpdatedAt())
            .createdBy(sd.getCreatedBy())
            .updatedBy(sd.getUpdatedBy())
            .build();
    }

    private static SchoolDirectoryOrigin sourceFromString(String origin) {
        return origin == null ? null : SchoolDirectoryOrigin.valueOf(origin);
    }

    private static String valueOf(SchoolDirectoryOrigin origin) {
        return origin == null ? null : origin.name();
    }
}
