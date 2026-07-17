package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.IdempotencyKey;
import com.sep.vox.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;

public final class IdempotencyKeyMapper {

    private IdempotencyKeyMapper() {}

    public static IdempotencyKey toDomain(IdempotencyKeyJpaEntity jpa) {
        return new IdempotencyKey(
            jpa.getId(),
            jpa.getKey(),
            jpa.getResultRef(),
            jpa.getCreatedAt()
        );
    }

    public static IdempotencyKeyJpaEntity toJpa(IdempotencyKey domain) {
        return new IdempotencyKeyJpaEntity(
            domain.getId(),
            domain.getKey(),
            domain.getResultRef(),
            domain.getCreatedAt()
        );
    }
}
