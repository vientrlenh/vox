package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.QuotaPricingCalibrationConfigPort;

/**
 * Parameters for QuotaPricingCalibrationJob/Service -- the job automatically recomputes
 * estimatedCostPerAssessmentSecondUsd (see QuotaPricingProperties) from the real ai_usage_record +
 * assessment_item_responses data, instead of having to edit .env by hand every time we want to recalibrate.
 */
@ConfigurationProperties(prefix = "vox.quota.calibration")
public record QuotaPricingCalibrationProperties(
    Integer windowDays,
    Integer minSampleSessions,
    BigDecimal maxChangeRatio,
    BigDecimal minRateBound,
    BigDecimal maxRateBound
) implements QuotaPricingCalibrationConfigPort {

    private static final int DEFAULT_WINDOW_DAYS = 90;
    private static final int DEFAULT_MIN_SAMPLE_SESSIONS = 10;
    private static final BigDecimal DEFAULT_MAX_CHANGE_RATIO = new BigDecimal("0.20");
    private static final BigDecimal DEFAULT_MIN_RATE_BOUND = new BigDecimal("0.001");
    private static final BigDecimal DEFAULT_MAX_RATE_BOUND = new BigDecimal("1.0");

    public QuotaPricingCalibrationProperties {
        windowDays = windowDays == null ? DEFAULT_WINDOW_DAYS : windowDays;
        minSampleSessions = minSampleSessions == null ? DEFAULT_MIN_SAMPLE_SESSIONS : minSampleSessions;
        maxChangeRatio = maxChangeRatio == null ? DEFAULT_MAX_CHANGE_RATIO : maxChangeRatio;
        minRateBound = minRateBound == null ? DEFAULT_MIN_RATE_BOUND : minRateBound;
        maxRateBound = maxRateBound == null ? DEFAULT_MAX_RATE_BOUND : maxRateBound;
    }
}
