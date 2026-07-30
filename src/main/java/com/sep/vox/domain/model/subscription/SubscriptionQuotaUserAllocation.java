package com.sep.vox.domain.model.subscription;

import java.util.UUID;

public class SubscriptionQuotaUserAllocation {
    private UUID id;
    private UUID subscriptionId;
    private QuotaType quotaType;
    private UUID userId;
    private Integer allocatedQuantity;
    private Integer usedQuantity;

    public SubscriptionQuotaUserAllocation() {}

    public SubscriptionQuotaUserAllocation(UUID id, UUID subscriptionId, QuotaType quotaType, UUID userId,
            Integer allocatedQuantity, Integer usedQuantity) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedQuantity = allocatedQuantity;
        this.usedQuantity = usedQuantity;
    }

    public SubscriptionQuotaUserAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId,
            Integer allocatedQuantity, Integer usedQuantity) {
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedQuantity = allocatedQuantity;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(Integer allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public Integer getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(Integer usedQuantity) {
        this.usedQuantity = usedQuantity;
    }
}
