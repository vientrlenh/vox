package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.QuotaPricingConfigPort;

/**
 * USD conversion factors used to estimate the worst-case AI cost before allowing an assessment to be
 * scheduled (see ClassTestTokenQuotaGuardService). This is an estimate, NOT the
 * real cost -- the real cost comes from ai_usage_record after the Agentic AI has finished processing.
 *
 * PLACEHOLDER: the default values below are assumptions, and must be replaced with real measurements
 * from ai_usage_record (average cost_usd / audio_duration_seconds over a long enough
 * period) before use in production.
 */
@ConfigurationProperties(prefix = "vox.quota")
public record QuotaPricingProperties(
    BigDecimal estimatedCostPerAssessmentSecondUsd,
    // Separate from estimatedCostPerAssessmentSecondUsd -- PRACTICE uses a much lighter AI pipeline than ASSESSMENT
    // (realtimeCorrectionGraph vs evalGraph), see QuotaPricingCalibrationService/QuotaPricingSource.
    BigDecimal estimatedCostPerPracticeSecondUsd
) implements QuotaPricingConfigPort {

    private static final BigDecimal DEFAULT_ESTIMATED_COST_PER_ASSESSMENT_SECOND_USD = new BigDecimal("0.01");
    private static final BigDecimal DEFAULT_ESTIMATED_COST_PER_PRACTICE_SECOND_USD = new BigDecimal("0.01");

    public QuotaPricingProperties {
        estimatedCostPerAssessmentSecondUsd = estimatedCostPerAssessmentSecondUsd == null
            ? DEFAULT_ESTIMATED_COST_PER_ASSESSMENT_SECOND_USD
            : estimatedCostPerAssessmentSecondUsd;
        estimatedCostPerPracticeSecondUsd = estimatedCostPerPracticeSecondUsd == null
            ? DEFAULT_ESTIMATED_COST_PER_PRACTICE_SECOND_USD
            : estimatedCostPerPracticeSecondUsd;
    }
}