package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.languagelevel.StandardLevel;
import com.sep.vox.domain.valueobject.LevelCode;
import com.sep.vox.infrastructure.persistence.entity.StandardLevelJpaEntity;

public final class StandardLevelMapper {

    public static StandardLevel toDomain(StandardLevelJpaEntity jpa) {
        return new StandardLevel(
            jpa.getId(), 
            jpa.getLanguageId(), 
            jpa.getFrameworkId(), 
            new LevelCode(jpa.getCode()), 
            jpa.getCurrentVersionId(), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static StandardLevelJpaEntity toJpa(StandardLevel level) {
        return new StandardLevelJpaEntity(
           level.getId(),
           level.getLanguageId(),
           level.getFrameworkId(),
           valueOf(level.getCode()),
           level.getCurrentVersionId(),
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
