package com.sep.vox.domain.model.subscription;

import java.util.UUID;

public class SubscriptionQuota {
    private UUID id;
    private UUID subscriptionId;
    private QuotaType quotaType;
    private Integer totalAllocated;
    private Integer usedQuantity;

    public SubscriptionQuota() {}

    public SubscriptionQuota(UUID id, UUID subscriptionId, QuotaType quotaType, Integer totalAllocated, Integer usedQuantity) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.totalAllocated = totalAllocated;
        this.usedQuantity = usedQuantity;
    }

    public SubscriptionQuota(UUID subscriptionId, QuotaType quotaType, Integer totalAllocated, Integer usedQuantity) {
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

    public Integer getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(Integer totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public Integer getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(Integer usedQuantity) {
        this.usedQuantity = usedQuantity;
    }
}
