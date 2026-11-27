package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Two parameters that produce the quota selling price, read INDEPENDENTLY of each other through QuotaPricingPort -- see
 * QuotaPricingService.
 *
 * <p>Before, these two numbers were pre-multiplied into ONE unit price (tokenUnitPriceVnd = usdToVndRate ×
 * (1 + serviceFeeRatio)) frozen on each quota line of a plan. That was dropped entirely: merged, a school
 * would see "1 USD = 31,200 VND" while the real rate is 26,000 VND, which cannot be reconciled with any
 * market rate. Now the service fee stands as its own line on the order, and the USD->VND conversion
 * is recorded per usage in SchoolBalanceEntry (costUsd + fxRateUsed).
 *
 * <p>usdToVndRate is now only a STATIC FALLBACK: the main source read is the latest snapshot that
 * ExchangeRateRefreshJob fetches from the real exchange rate API (see ExchangeRateApiProperties /
 * QuotaPricingService.usdToVndRate()) -- the value here (VOX_QUOTA_USD_TO_VND_RATE) is only used when there is
 * no snapshot yet (just deployed, job has never run).
 *
 * <p>serviceFeeRatio is the opposite, it is the ONLY source. It used to be that each SubscriptionPlan held its own
 * ratio, but the service_fee_ratio column was dropped from the plan: a replaced plan
 * (replacedByPlanId) gets a new id, so with the margin living per plan every price change had to be copied by hand
 * to the new plan and it was very easy to drift between plans sold at the same time.
 */
@ConfigurationProperties(prefix = "vox.quota.selling-price")
public record QuotaSellingPriceProperties(BigDecimal usdToVndRate, BigDecimal serviceFeeRatio) {

    private static final BigDecimal DEFAULT_USD_TO_VND_RATE = new BigDecimal("26000");
    private static final BigDecimal DEFAULT_SERVICE_FEE_RATIO = new BigDecimal("0.05"); // default changed to 5%, 20% is currently far too much compared with the estimate

    public QuotaSellingPriceProperties {
        usdToVndRate = usdToVndRate == null ? DEFAULT_USD_TO_VND_RATE : usdToVndRate;
        serviceFeeRatio = serviceFeeRatio == null ? DEFAULT_SERVICE_FEE_RATIO : serviceFeeRatio;
    }
}
