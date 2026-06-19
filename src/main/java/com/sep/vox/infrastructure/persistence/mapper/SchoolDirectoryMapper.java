package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.model.school.SchoolDirectorySource;
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
            sourceFromString(jpa.getSource()), 
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
            valueOf(sd.getSource()), 
            sd.getCreatedAt(), 
            sd.getUpdatedAt(), 
            sd.getCreatedBy(), 
            sd.getUpdatedBy()
        );
    }

    private static SchoolDirectorySource sourceFromString(String source) {
        return source == null ? null : SchoolDirectorySource.valueOf(source);
    }

    private static String valueOf(SchoolDirectorySource source) {
        return source == null ? null : source.name();
    }
}
