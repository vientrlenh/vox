package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

public class SchoolSubscriptionQuotaUserAllocation {
    private UUID id;
    private UUID schoolSubscriptionId;
    private QuotaType quotaType;
    private UUID userId;
    private BigDecimal allocatedQuantity;
    private BigDecimal usedQuantity;

    public SchoolSubscriptionQuotaUserAllocation() {}

    public SchoolSubscriptionQuotaUserAllocation(UUID id, UUID schoolSubscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal allocatedQuantity, BigDecimal usedQuantity) {
        this.id = id;
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedQuantity = allocatedQuantity;
        this.usedQuantity = usedQuantity;
    }

    public SchoolSubscriptionQuotaUserAllocation(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal allocatedQuantity, BigDecimal usedQuantity) {
        this.schoolSubscriptionId = schoolSubscriptionId;
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

    public UUID getSchoolSubscriptionId() {
        return schoolSubscriptionId;
    }

    public void setSchoolSubscriptionId(UUID schoolSubscriptionId) {
        this.schoolSubscriptionId = schoolSubscriptionId;
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