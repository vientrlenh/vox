package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.infrastructure.persistence.entity.ExchangeRateSnapshotJpaEntity;

public final class ExchangeRateSnapshotMapper {

    private ExchangeRateSnapshotMapper() {}

    public static ExchangeRateSnapshot toDomain(ExchangeRateSnapshotJpaEntity jpa) {
        return new ExchangeRateSnapshot(
            jpa.getId(),
            jpa.getFetchedAt(),
            jpa.getUsdToVndRate(),
            jpa.getSource()
        );
    }

    public static ExchangeRateSnapshotJpaEntity toJpa(ExchangeRateSnapshot domain) {
        return new ExchangeRateSnapshotJpaEntity(
            domain.getId(),
            domain.getFetchedAt(),
            domain.getUsdToVndRate(),
            domain.getSource()
        );
    }
}
