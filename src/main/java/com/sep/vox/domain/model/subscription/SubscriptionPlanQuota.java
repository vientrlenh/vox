package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * The amount of one quota type included in the plan price, in VND.
 *
 * <p>There is no tokenUnitPrice any more: the old unit price "VND per $1 of quota" was an exchange rate WITH THE
 * MARGIN ALREADY ADDED (fx x (1 + serviceFeeRatio)), so the school saw an exchange rate that did not match the market.
 * The margin is now a separate line on the order, and the USD->VND conversion is recorded per usage in
 * SchoolBalanceEntry (costUsd + fxRateUsed).
 */
public class SubscriptionPlanQuota {
    private UUID id;
    private UUID subscriptionPlanId;
    private QuotaType quotaType;
    private BigDecimal includedAmountVnd;

    public SubscriptionPlanQuota() {}



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getIncludedAmountVnd() {
        return includedAmountVnd;
    }

    public void setIncludedAmountVnd(BigDecimal includedAmountVnd) {
        this.includedAmountVnd = includedAmountVnd;
    }

    public static SubscriptionPlanQuota forPlan(UUID subscriptionPlanId, QuotaType quotaType, BigDecimal includedAmountVnd) {
        return builder()
                .subscriptionPlanId(subscriptionPlanId)
                .quotaType(quotaType)
                .includedAmountVnd(includedAmountVnd)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SubscriptionPlanQuota target = new SubscriptionPlanQuota();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder subscriptionPlanId(UUID subscriptionPlanId) {
            target.subscriptionPlanId = subscriptionPlanId;
            return this;
        }

        public Builder quotaType(QuotaType quotaType) {
            target.quotaType = quotaType;
            return this;
        }

        public Builder includedAmountVnd(BigDecimal includedAmountVnd) {
            target.includedAmountVnd = includedAmountVnd;
            return this;
        }

        public SubscriptionPlanQuota build() {
            return target;
        }

    }
}
