package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.metering.QuotaType;

@Service
public class SubscriptionPlanQuotaAmountValidator {

    // Column subscription_plan_quotas.included_amount_vnd is numeric(18,6) -- above this limit Postgres
    // reports "numeric field overflow" at the DB layer instead of a readable message here.
    private static final BigDecimal MAX_INCLUDED_AMOUNT_VND = new BigDecimal("999999999999.999999");

    public void requireValidIncludedAmount(QuotaType quotaType, @Nullable BigDecimal includedAmountVnd) {
        if (includedAmountVnd == null) {
            throw new IllegalArgumentException("The amount of quota \"" + quotaType + "\" must not be blank");
        }
        if (includedAmountVnd.signum() < 0) {
            throw new IllegalArgumentException("The amount of quota \"" + quotaType + "\" must not be negative");
        }
        if (includedAmountVnd.compareTo(MAX_INCLUDED_AMOUNT_VND) > 0) {
            throw new IllegalArgumentException(
                "The amount of quota \"" + quotaType + "\" exceeds the allowed limit (maximum 999,999,999,999 VND)");
        }
    }
}
