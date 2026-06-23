package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;
import com.sep.vox.infrastructure.persistence.entity.SchoolDirectoryJpaEntity;

public final class SchoolDirectoryMapper {
    

    public static SchoolDirectory toDomain(SchoolDirectoryJpaEntity jpa) {
        return new SchoolDirectory(
            jpa.getId(), 
            jpa.getCode(), 
            jpa.getName(), 
            jpa.getProvinceCode(), 
            jpa.getDistrictName(), 
            jpa.getDistrictName(), 
            jpa.getDomain(), 
            jpa.getAddress(), 
            sourceFromString(jpa.getOrigin()), 
            jpa.isVerified(),
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static SchoolDirectoryJpaEntity toJpa(SchoolDirectory sd) {
        return new SchoolDirectoryJpaEntity(
            sd.getId(), 
            sd.getCode(), 
            sd.getName(), 
            sd.getProvinceCode(), 
            sd.getProvinceName(), 
            sd.getDistrictName(), 
            sd.getDomain(), 
            sd.getAddress(), 
            valueOf(sd.getOrigin()), 
            sd.isVerified(),
            sd.getCreatedAt(), 
            sd.getUpdatedAt(), 
            sd.getCreatedBy(), 
            sd.getUpdatedBy()
        );
    }

    private static SchoolDirectoryOrigin sourceFromString(String origin) {
        return origin == null ? null : SchoolDirectoryOrigin.valueOf(origin);
    }

    private static String valueOf(SchoolDirectoryOrigin origin) {
        return origin == null ? null : origin.name();
    }
}
