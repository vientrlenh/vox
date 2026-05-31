package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.languagelevel.LevelFramework;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.infrastructure.persistence.entity.LevelFrameworkJpaEntity;

public final class LevelFrameworkMapper {
    
    public static LevelFramework toDomain(LevelFrameworkJpaEntity jpa) {
        return new LevelFramework(
            jpa.getId(), 
            new FrameworkCode(jpa.getCode()), 
            jpa.getName(), 
            jpa.getDescription(), 
            jpa.isActive(), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static LevelFrameworkJpaEntity toJpa(LevelFramework framework) {
        return new LevelFrameworkJpaEntity(
            framework.getId(), 
            valueOf(framework.getCode()), 
            framework.getName(), 
            framework.getDescription(), 
            framework.isActive(), 
            framework.getCreatedAt(), 
            framework.getUpdatedAt(), 
            framework.getCreatedBy(),
            framework.getUpdatedBy()
        );
    }

    private static String valueOf(FrameworkCode code) {
        return code == null ? null : code.value();
    }
}
