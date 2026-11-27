package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sep.vox.application.port.output.QuotaDebtConfigPort;

/**
 * Warning ceiling for school debt (a negative self-funded wallet balance -- see ConsumeQuotaService.checkDebtCapTransition
 * and SchoolSubscriptionDebtGuardService). ONLY for an early warning when the AI cost measuring pipeline has a bug that makes
 * debt grow abnormally -- it does NOT block quota deduction (the real cost must always be recorded correctly) and
 * does NOT affect the automatic unlock mechanism when the school tops up and brings the balance back to non-negative.
 */
@ConfigurationProperties(prefix = "vox.quota.debt")
public record QuotaDebtProperties(
    BigDecimal capRatio
) implements QuotaDebtConfigPort {

    private static final BigDecimal DEFAULT_CAP_RATIO = new BigDecimal("0.20");

    public QuotaDebtProperties {
        capRatio = capRatio == null ? DEFAULT_CAP_RATIO : capRatio;
    }
}
