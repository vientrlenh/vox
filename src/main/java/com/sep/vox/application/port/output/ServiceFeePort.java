package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * A READ port for the service fee ratio for the application/interfaces layers. The real source of
 * the number is a system-wide config ({@code vox.quota.selling-price.service-fee-ratio}) in
 * infrastructure -- see QuotaPricingService.
 *
 * <p>Deliberately separated from {@link QuotaPricingPort} even though it has the same implementation:
 * that port returns COST PRICE (estimated AI cost per second, the USD->VND rate) and is exposed as
 * is through the GraphQL query {@code quotaPricing} for schools to see. This number is our own
 * MARGIN -- it may only appear already added into a fee line on an order, not as a field that can
 * be looked up.
 *
 * <p>Thanks to the split, the top-up order use case depends only on the one number it needs,
 * instead of pulling in the assessment/practice costs that it never reads.
 *
 * <p>The implementation handles the fallback inside itself (.env config -> default constant), so it
 * always returns a usable value, never null.
 */
public interface ServiceFeePort {

    /**
     * The service fee ratio (e.g. 0.20 = 20%) that an ORDER adds on top of the quota amount.
     *
     * <p>DELIBERATELY not folded into the exchange rate: folding it in would show "1 USD = 31,200 VND"
     * when the real rate is 26,000 VND, and the school could not compare it against any market rate.
     * The fee must be its own line on the order so it is obvious what is being paid for.
     *
     * <p>It is a SYSTEM-WIDE config and is not stored per plan (the service_fee_ratio column was
     * removed from SubscriptionPlan) -- see QuotaSellingPriceProperties.
     */
    BigDecimal serviceFeeRatio();
}
