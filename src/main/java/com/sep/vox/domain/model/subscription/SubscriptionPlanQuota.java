package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

public class SubscriptionPlanQuota {
    private UUID id;
    private UUID subscriptionPlanId;
    private QuotaType quotaType;
    private BigDecimal includedQuantity;
    private BigDecimal tokenUnitPrice;

    public SubscriptionPlanQuota() {}

    public SubscriptionPlanQuota(UUID id, UUID subscriptionPlanId, QuotaType quotaType, BigDecimal includedQuantity, BigDecimal tokenUnitPrice) {
        this.id = id;
        this.subscriptionPlanId = subscriptionPlanId;
        this.quotaType = quotaType;
        this.includedQuantity = includedQuantity;
        this.tokenUnitPrice = tokenUnitPrice;
    }

    public SubscriptionPlanQuota(UUID subscriptionPlanId, QuotaType quotaType, BigDecimal includedQuantity, BigDecimal tokenUnitPrice) {
        this.subscriptionPlanId = subscriptionPlanId;
        this.quotaType = quotaType;
        this.includedQuantity = includedQuantity;
        this.tokenUnitPrice = tokenUnitPrice;
    }

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

    public BigDecimal getIncludedQuantity() {
        return includedQuantity;
    }

    public void setIncludedQuantity(BigDecimal includedQuantity) {
        this.includedQuantity = includedQuantity;
    }

    public BigDecimal getTokenUnitPrice() {
        return tokenUnitPrice;
    }

    public void setTokenUnitPrice(BigDecimal tokenUnitPrice) {
        this.tokenUnitPrice = tokenUnitPrice;
    }
}
