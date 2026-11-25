package com.sep.vox.domain.model.metering;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class QuotaPricingCalibrationTests {

    @Test
    void computedSetsEveryArgument() {
        Instant computedAt = Instant.parse("2026-11-25T01:00:00Z");

        QuotaPricingCalibration calibration = QuotaPricingCalibration.computed(
                computedAt, 90, 12, new BigDecimal("3.5"), 600L, new BigDecimal("0.0058"), new BigDecimal("0.0050"),
                "clamped", QuotaPricingSource.PRACTICE);

        assertThat(calibration.getId()).isNull();
        assertThat(calibration.getComputedAt()).isEqualTo(computedAt);
        assertThat(calibration.getWindowDays()).isEqualTo(90);
        assertThat(calibration.getSessionCount()).isEqualTo(12);
        assertThat(calibration.getTotalCostUsd()).isEqualByComparingTo("3.5");
        assertThat(calibration.getTotalAnsweredSeconds()).isEqualTo(600L);
        assertThat(calibration.getRawRateUsdPerSecond()).isEqualByComparingTo("0.0058");
        assertThat(calibration.getAppliedRateUsdPerSecond()).isEqualByComparingTo("0.0050");
        assertThat(calibration.getNote()).isEqualTo("clamped");
        assertThat(calibration.getPricingSource()).isEqualTo(QuotaPricingSource.PRACTICE);
    }
}
