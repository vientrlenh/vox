package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.QuotaUsageWarningConfigPort;

/**
 * The EARLY warning threshold (NOT blocking) for when the school's AI quota (ASSESSMENT/PRACTICE) has only used up a certain
 * ratio -- see ConsumeQuotaService.checkUsageWarningTransition. The purpose is for the school to know in advance
 * and renew/top up, BEFORE the quota runs out and starts counting as debt (see QuotaDebtProperties).
 */
@ConfigurationProperties(prefix = "vox.quota.usage-warning")
public record QuotaUsageWarningProperties(
    BigDecimal warningRatio
) implements QuotaUsageWarningConfigPort {

    private static final BigDecimal DEFAULT_WARNING_RATIO = new BigDecimal("0.70");

    public QuotaUsageWarningProperties {
        warningRatio = warningRatio == null ? DEFAULT_WARNING_RATIO : warningRatio;
    }
}
