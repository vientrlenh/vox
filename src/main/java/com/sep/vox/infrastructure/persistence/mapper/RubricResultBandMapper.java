package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.infrastructure.persistence.entity.RubricResultBandJpaEntity;

public final class RubricResultBandMapper {
    
    public static RubricResultBand toDomain(RubricResultBandJpaEntity jpa) {
        return new RubricResultBand(
            jpa.getId(),
            jpa.getRubricVersionId(), 
            jpa.getFrameworkResultBandId(),
            jpa.getCode(), 
            jpa.getName(), 
            jpa.getDescription(), 
            jpa.getMappedScoreMin(), 
            jpa.getMappedScoreMax(), 
            jpa.getOrder(), 
            jpa.getIsPassing(), 
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy()
        );
    }

    public static RubricResultBandJpaEntity toJpa(RubricResultBand band) {
        return new RubricResultBandJpaEntity(
            band.getId(), 
            band.getRubricVersionId(), 
            band.getCode(), 
            band.getName(), 
            band.getDescription(), 
            band.getMappedScoreMin(), 
            band.getMappedScoreMax(), 
            band.getOrder(), 
            band.getIsPassing(), 
            band.getFrameworkResultBandId(), 
            band.getCreatedAt(), 
            band.getUpdatedAt(), 
            band.getCreatedBy(), 
            band.getUpdatedBy()
        );
    }
}
