package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

public class SubscriptionQuotaUserAllocation {
    private UUID id;
    private UUID subscriptionId;
    private QuotaType quotaType;
    private UUID userId;
    private BigDecimal allocatedQuantity;
    private BigDecimal usedQuantity;

    public SubscriptionQuotaUserAllocation() {}

    public SubscriptionQuotaUserAllocation(UUID id, UUID subscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal allocatedQuantity, BigDecimal usedQuantity) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedQuantity = allocatedQuantity;
        this.usedQuantity = usedQuantity;
    }

    public SubscriptionQuotaUserAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal allocatedQuantity, BigDecimal usedQuantity) {
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

    public BigDecimal getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(BigDecimal allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public BigDecimal getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(BigDecimal usedQuantity) {
        this.usedQuantity = usedQuantity;
    }
}