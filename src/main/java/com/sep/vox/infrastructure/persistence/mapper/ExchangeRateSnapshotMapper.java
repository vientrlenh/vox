package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.infrastructure.persistence.entity.financial.ExchangeRateSnapshotJpaEntity;

public final class ExchangeRateSnapshotMapper {

    private ExchangeRateSnapshotMapper() {}

    public static ExchangeRateSnapshot toDomain(ExchangeRateSnapshotJpaEntity jpa) {
        return ExchangeRateSnapshot.builder()
            .id(jpa.getId())
            .currencyCode(fromString(jpa.getCurrencyCode()))
            .exchangeRateToVnd(jpa.getExchangeRateToVnd())
            .fetchedAt(jpa.getFetchedAt())
            .sourceUrl(jpa.getSourceUrl())
            .build();
    }

    public static ExchangeRateSnapshotJpaEntity toJpa(ExchangeRateSnapshot domain) {
        return ExchangeRateSnapshotJpaEntity.builder()
            .id(domain.getId())
            .currencyCode(valueOf(domain.getCurrencyCode()))
            .exchangeRateToVnd(domain.getExchangeRateToVnd())
            .fetchedAt(domain.getFetchedAt())
            .sourceUrl(domain.getSourceUrl())
            .build();
    }

    private static CurrencyCode fromString(String code) {
        if (code == null) 
            return null;
        try {
            return CurrencyCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency value when converting to the domain model: " + code);
        }
    }

    private static String valueOf(CurrencyCode code) {
        return code == null ? null : code.name();
    }
}
