package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.infrastructure.persistence.entity.ExchangeRateSnapshotJpaEntity;

public final class ExchangeRateSnapshotMapper {

    private ExchangeRateSnapshotMapper() {}

    public static ExchangeRateSnapshot toDomain(ExchangeRateSnapshotJpaEntity jpa) {
        return new ExchangeRateSnapshot(
            jpa.getId(),
            fromString(jpa.getCurrencyCode()), 
            jpa.getExchangeRateToVnd(),
            jpa.getFetchedAt(),
            jpa.getSourceUrl()
        );
    }

    public static ExchangeRateSnapshotJpaEntity toJpa(ExchangeRateSnapshot domain) {
        return new ExchangeRateSnapshotJpaEntity(
            domain.getId(), 
            valueOf(domain.getCurrencyCode()), 
            domain.getExchangeRateToVnd(),
            domain.getFetchedAt(),
            domain.getSourceUrl()
        );
    }

    private static CurrencyCode fromString(String code) {
        if (code == null) 
            return null;
        try {
            return CurrencyCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Giá trị tiền tệ khi convert domain model không hợp lệ: " + code);
        }
    }

    private static String valueOf(CurrencyCode code) {
        return code == null ? null : code.name();
    }
}
