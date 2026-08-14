package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.QuotaPricingCalibration;
import com.sep.vox.domain.model.subscription.QuotaPricingSource;
import com.sep.vox.infrastructure.persistence.entity.QuotaPricingCalibrationJpaEntity;

public final class QuotaPricingCalibrationMapper {

    private QuotaPricingCalibrationMapper() {}

    public static QuotaPricingCalibration toDomain(QuotaPricingCalibrationJpaEntity jpa) {
        return new QuotaPricingCalibration(
            jpa.getId(),
            jpa.getComputedAt(),
            jpa.getWindowDays(),
            jpa.getSessionCount(),
            jpa.getTotalCostUsd(),
            jpa.getTotalAnsweredSeconds(),
            jpa.getRawRateUsdPerSecond(),
            jpa.getAppliedRateUsdPerSecond(),
            jpa.getNote(),
            QuotaPricingSource.valueOf(jpa.getPricingSource())
        );
    }

    public static QuotaPricingCalibrationJpaEntity toJpa(QuotaPricingCalibration domain) {
        return new QuotaPricingCalibrationJpaEntity(
            domain.getId(),
            domain.getComputedAt(),
            domain.getWindowDays(),
            domain.getSessionCount(),
            domain.getTotalCostUsd(),
            domain.getTotalAnsweredSeconds(),
            domain.getRawRateUsdPerSecond(),
            domain.getAppliedRateUsdPerSecond(),
            domain.getNote(),
            domain.getPricingSource().name()
        );
    }
}
