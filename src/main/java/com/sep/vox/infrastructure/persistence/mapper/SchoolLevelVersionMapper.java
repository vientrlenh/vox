package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.languagelevel.LevelMappingType;
import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.languagelevel.SchoolLevelVersion;
import com.sep.vox.domain.valueobject.LevelOrder;
import com.sep.vox.domain.valueobject.LevelVersion;
import com.sep.vox.infrastructure.persistence.entity.SchoolLevelVersionJpaEntity;

public final class SchoolLevelVersionMapper {
    
    public static SchoolLevelVersion toDomain(SchoolLevelVersionJpaEntity jpa) {
        return new SchoolLevelVersion(
            jpa.getId(), 
            jpa.getSchoolLevelId(), 
            new LevelVersion(jpa.getVersion()), 
            jpa.getName(), 
            jpa.getDescription(), 
            mappingTypeFromString(jpa.getMappingType()), 
            jpa.getMappedStandardLevelVersionId(), 
            jpa.getMappedStandardLevelMinVersionId(), 
            jpa.getMappedStandardLevelMaxVersionId(), 
            statusFromString(jpa.getStatus()), 
            new LevelOrder(jpa.getOrder()), 
            jpa.getExpectedAbilities(), 
            jpa.getLimitations(), 
            jpa.getEffectiveFrom(), 
            jpa.getEffectiveTo(), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static SchoolLevelVersionJpaEntity toJpa(SchoolLevelVersion version) {
        return new SchoolLevelVersionJpaEntity(
            version.getId(),
            version.getSchoolLevelId(),
            version.getVersion().value(),
            version.getName(),
            version.getDescription(),
            valueOf(version.getMappingType()),
            version.getMappedStandardLevelVersionId(), 
            version.getMappedStandardLevelMinVersionId(), 
            version.getMappedStandardLevelMaxVersionId(), 
            valueOf(version.getStatus()),
            version.getOrder().value(),
            version.getExpectedAbilities(), 
            version.getLimitations(), 
            version.getEffectiveFrom(), 
            version.getEffectiveTo(), 
            version.getCreatedAt(), 
            version.getUpdatedAt(), 
            version.getCreatedBy(), 
            version.getUpdatedBy()
        );
    }

    private static LevelMappingType mappingTypeFromString(String mappingType) {
        return mappingType == null ? null : LevelMappingType.valueOf(mappingType);
    }

    private static LevelStatus statusFromString(String levelStatus) {
        return levelStatus == null ? null : LevelStatus.valueOf(levelStatus);
    }

    private static String valueOf(LevelMappingType mappingType) {
        return mappingType == null ? null : mappingType.name();
    }

    private static String valueOf(LevelStatus status) {
        return status == null ? null : status.name();
    }
}
