package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * The static price constants in .env that the application needs to read -- the FALLBACK level when
 * no calibration has succeeded yet. The real values are bound from application.yaml
 * ({@code vox.quota.*}) in QuotaPricingProperties.
 *
 * <p>Different from QuotaPricingPort: that port returns the price CURRENTLY in effect (already chosen
 * between the calibrated and the static numbers), while this interface only exposes the raw static
 * numbers for QuotaPricingCalibrationService to use as a comparison baseline.
 */
public interface QuotaPricingConfigPort {

    BigDecimal estimatedCostPerAssessmentSecondUsd();

    BigDecimal estimatedCostPerPracticeSecondUsd();
}
