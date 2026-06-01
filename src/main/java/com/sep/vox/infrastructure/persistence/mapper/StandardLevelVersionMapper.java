package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.languagelevel.StandardLevelVersion;
import com.sep.vox.domain.valueobject.LevelDifficulty;
import com.sep.vox.domain.valueobject.LevelOrder;
import com.sep.vox.domain.valueobject.LevelVersion;
import com.sep.vox.infrastructure.persistence.entity.StandardLevelVersionJpaEntity;

public final class StandardLevelVersionMapper {
    
    public static StandardLevelVersion toDomain(StandardLevelVersionJpaEntity jpa) {
        return new StandardLevelVersion(
            jpa.getId(), 
            jpa.getStandardLevelId(), 
            new LevelVersion(jpa.getVersion()), 
            jpa.getName(), 
            jpa.getDescription(), 
            new LevelOrder(jpa.getOrder()), 
            new LevelDifficulty(jpa.getDifficultyMin()), 
            new LevelDifficulty(jpa.getDifficultyMax()), 
            fromString(jpa.getStatus()), 
            jpa.getEffectiveFrom(), 
            jpa.getEffectiveTo(), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static StandardLevelVersionJpaEntity toJpa(StandardLevelVersion version) {
        return new StandardLevelVersionJpaEntity(
            version.getId(), 
            version.getStandardLevelId(), 
            version.getVersion().value(), 
            version.getName(), 
            version.getDescription(), 
            version.getOrder().value(), 
            version.getDifficultyMin().value(), 
            version.getDifficultyMax().value(), 
            version.getStatus().name(), 
            version.getEffectiveFrom(), 
            version.getEffectiveTo(), 
            version.getCreatedAt(), 
            version.getUpdatedAt(), 
            version.getCreatedBy(), 
            version.getUpdatedBy()
        );
    }

    private static LevelStatus fromString(String value) {
        return value == null ? null : LevelStatus.valueOf(value);
    }


}
