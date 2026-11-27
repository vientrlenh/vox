package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * Parameters for QuotaPricingCalibrationService -- the data window, the minimum sample size and the
 * safety margin when the per-second price is recalculated automatically. The real values are bound
 * from application.yaml ({@code vox.quota.calibration.*}) in QuotaPricingCalibrationProperties.
 */
public interface QuotaPricingCalibrationConfigPort {

    Integer windowDays();

    Integer minSampleSessions();

    BigDecimal maxChangeRatio();

    BigDecimal minRateBound();

    BigDecimal maxRateBound();
}
