package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * A READ port for pricing config for the application/interfaces layers -- the real source of the
 * numbers (properties in .env + a snapshot in the DB) lives in infrastructure, so use cases depend
 * only on this interface instead of importing back up into the implementation (see QuotaPricingService).
 *
 * <p>Every method already handles the fallback inside the implementation (calibrated price -> .env
 * constant, exchange-rate snapshot -> .env constant), so it always returns a usable value, never null.
 */
public interface QuotaPricingPort {

    /** Estimated price per assessment second (USD) -- used by ClassTestTokenQuotaGuardService. */
    BigDecimal currentEstimatedCostPerAssessmentSecondUsd();

    /** Estimated price per practice second (USD) -- used by BuildPracticePaperUseCase. */
    BigDecimal currentEstimatedCostPerPracticeSecondUsd();

    /** The market USD->VND exchange rate currently in effect, shared by every plan. */
    BigDecimal usdToVndRate();

    // serviceFeeRatio() moved to ServiceFeePort: every remaining method here is COST PRICE and is
    // exposed as is through the GraphQL query quotaPricing, while the service fee is our own margin.
}
