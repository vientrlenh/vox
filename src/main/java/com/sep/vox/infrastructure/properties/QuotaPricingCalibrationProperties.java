package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tham số cho QuotaPricingCalibrationJob/Service -- job tự tính lại
 * estimatedCostPerExamSecondUsd (xem QuotaPricingProperties) từ dữ liệu ai_usage_record +
 * exam_item_responses thật, thay vì phải tự tay sửa .env mỗi lần muốn calibrate lại.
 */
@ConfigurationProperties(prefix = "vox.quota.calibration")
public record QuotaPricingCalibrationProperties(
    Integer windowDays,
    Integer minSampleSessions,
    BigDecimal maxChangeRatio,
    BigDecimal minRateBound,
    BigDecimal maxRateBound
) {

    private static final int DEFAULT_WINDOW_DAYS = 90;
    private static final int DEFAULT_MIN_SAMPLE_SESSIONS = 30;
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
