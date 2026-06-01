package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.languagelevel.SchoolLevel;
import com.sep.vox.domain.valueobject.LevelCode;
import com.sep.vox.infrastructure.persistence.entity.SchoolLevelJpaEntity;

public final class SchoolLevelMapper {
    
    public static SchoolLevel toDomain(SchoolLevelJpaEntity jpa) {
        return new SchoolLevel(
            jpa.getId(), 
            jpa.getSchoolId(), 
            jpa.getLanguageId(), 
            jpa.getFrameworkId(), 
            new LevelCode(jpa.getCode()), 
            jpa.getCurrentSchoolLevelVersionId(), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static SchoolLevelJpaEntity toJpa(SchoolLevel level) {
        return new SchoolLevelJpaEntity(
            level.getId(), 
            level.getSchoolId(),
            level.getLanguageId(), 
            level.getFrameworkId(), 
            valueOf(level.getCode()), 
            level.getCurrentSchoolLevelVersionId(), 
            level.getCreatedAt(), 
            level.getUpdatedAt(), 
            level.getCreatedBy(), 
            level.getUpdatedBy()
        );
    }

    private static String valueOf(LevelCode code) {
        return code == null ? null : code.value();
    }
}
