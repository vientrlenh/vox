package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;

/**
 * The COST price currently in effect, exposed in full for the school to view -- see QuotaPricingPort.
 *
 * <p>There is NO serviceFeeRatio: the service fee is our profit margin and may only appear as one
 * fee line added to an order, not as a field that can be looked up. That is also why it lives in
 * a separate ServiceFeePort instead of sharing a port with these three numbers.
 *
 * @param estimatedCostPerAssessmentSecondUsd the estimated price per assessment second, preferring the number automatically calibrated from
 *                                          the real cost (QuotaPricingCalibrationJob)
 * @param estimatedCostPerPracticeSecondUsd as above but for speaking practice -- a very different AI pipeline so
 *                                          calibrated separately
 * @param usdToVndRate                      the market exchange rate, WITHOUT margin: folding the margin in here would produce
 *                                          a rate the school cannot reconcile against any
 *                                          external source
 */
public record QuotaPricingResponse(
    BigDecimal estimatedCostPerAssessmentSecondUsd,
    BigDecimal estimatedCostPerPracticeSecondUsd,
    BigDecimal usdToVndRate
) {
}
