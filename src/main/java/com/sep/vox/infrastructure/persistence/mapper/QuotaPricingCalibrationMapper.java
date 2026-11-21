package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaPricingCalibration;
import com.sep.vox.domain.model.metering.QuotaPricingSource;
import com.sep.vox.infrastructure.persistence.entity.metering.QuotaPricingCalibrationJpaEntity;

public final class QuotaPricingCalibrationMapper {

    private QuotaPricingCalibrationMapper() {}

    public static QuotaPricingCalibration toDomain(QuotaPricingCalibrationJpaEntity jpa) {
        return QuotaPricingCalibration.builder()
            .id(jpa.getId())
            .computedAt(jpa.getComputedAt())
            .windowDays(jpa.getWindowDays())
            .sessionCount(jpa.getSessionCount())
            .totalCostUsd(jpa.getTotalCostUsd())
            .totalAnsweredSeconds(jpa.getTotalAnsweredSeconds())
            .rawRateUsdPerSecond(jpa.getRawRateUsdPerSecond())
            .appliedRateUsdPerSecond(jpa.getAppliedRateUsdPerSecond())
            .note(jpa.getNote())
            .pricingSource(QuotaPricingSource.valueOf(jpa.getPricingSource()))
            .build();
    }

    public static QuotaPricingCalibrationJpaEntity toJpa(QuotaPricingCalibration domain) {
        return QuotaPricingCalibrationJpaEntity.builder()
            .id(domain.getId())
            .computedAt(domain.getComputedAt())
            .windowDays(domain.getWindowDays())
            .sessionCount(domain.getSessionCount())
            .totalCostUsd(domain.getTotalCostUsd())
            .totalAnsweredSeconds(domain.getTotalAnsweredSeconds())
            .rawRateUsdPerSecond(domain.getRawRateUsdPerSecond())
            .appliedRateUsdPerSecond(domain.getAppliedRateUsdPerSecond())
            .note(domain.getNote())
            .pricingSource(domain.getPricingSource().name())
            .build();
    }
}
