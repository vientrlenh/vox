package com.sep.vox.infrastructure.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for ExchangeRateApiClient/ExchangeRateRefreshJob -- the job that periodically fetches the real
 * USD->VND rate from an external API, replacing the hand-typed static constant in QuotaSellingPriceProperties.
 *
 * <p>minRateBound/maxRateBound are a safety bound: if the API returns a value outside this range (provider
 * error, malformed response, ...) then ExchangeRateApiClient treats it as a failed fetch and does NOT save a
 * snapshot -- so that one garbage response from the API cannot corrupt the real quota selling price (this rate goes into the quota money
 * on orders and into SchoolBalanceEntry.fxRateUsed of every usage -- see
 * QuotaPricingPort.usdToVndRate).
 */
@ConfigurationProperties(prefix = "vox.quota.exchange-rate-api")
public record ExchangeRateApiProperties(String baseUrl, BigDecimal minRateBound, BigDecimal maxRateBound) {

    private static final String DEFAULT_BASE_URL = "https://open.er-api.com/v6/latest/USD";
    private static final BigDecimal DEFAULT_MIN_RATE_BOUND = new BigDecimal("15000");
    private static final BigDecimal DEFAULT_MAX_RATE_BOUND = new BigDecimal("40000");

    public ExchangeRateApiProperties {
        baseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl;
        minRateBound = minRateBound == null ? DEFAULT_MIN_RATE_BOUND : minRateBound;
        maxRateBound = maxRateBound == null ? DEFAULT_MAX_RATE_BOUND : maxRateBound;
    }
}
