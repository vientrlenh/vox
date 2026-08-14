package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

public class SubscriptionQuota {
    private UUID id;
    private UUID subscriptionId;
    private QuotaType quotaType;
    private BigDecimal totalAllocated;
    private BigDecimal usedQuantity;

    public SubscriptionQuota() {}

    public SubscriptionQuota(UUID id, UUID subscriptionId, QuotaType quotaType, BigDecimal totalAllocated, BigDecimal usedQuantity) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.totalAllocated = totalAllocated;
        this.usedQuantity = usedQuantity;
    }

    public SubscriptionQuota(UUID subscriptionId, QuotaType quotaType, BigDecimal totalAllocated, BigDecimal usedQuantity) {
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.totalAllocated = totalAllocated;
        this.usedQuantity = usedQuantity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(BigDecimal totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public BigDecimal getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(BigDecimal usedQuantity) {
        this.usedQuantity = usedQuantity;
    }
}