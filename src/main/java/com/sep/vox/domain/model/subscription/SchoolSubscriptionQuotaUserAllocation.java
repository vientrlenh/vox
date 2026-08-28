package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

public class SchoolSubscriptionQuotaUserAllocation {
    private UUID id;
    private UUID schoolSubscriptionId;
    private QuotaType quotaType;
    private UUID userId;
    private BigDecimal allocatedAmountVnd;
    private BigDecimal usedAmountVnd;

    public SchoolSubscriptionQuotaUserAllocation() {}

    public SchoolSubscriptionQuotaUserAllocation(UUID id, UUID schoolSubscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal allocatedAmountVnd, BigDecimal usedAmountVnd) {
        this.id = id;
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedAmountVnd = allocatedAmountVnd;
        this.usedAmountVnd = usedAmountVnd;
    }

    public SchoolSubscriptionQuotaUserAllocation(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal allocatedAmountVnd, BigDecimal usedAmountVnd) {
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedAmountVnd = allocatedAmountVnd;
        this.usedAmountVnd = usedAmountVnd;
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

    public BigDecimal getAllocatedAmountVnd() {
        return allocatedAmountVnd;
    }

    public void setAllocatedAmountVnd(BigDecimal allocatedAmountVnd) {
        this.allocatedAmountVnd = allocatedAmountVnd;
    }

    public BigDecimal getUsedAmountVnd() {
        return usedAmountVnd;
    }

    public void setUsedAmountVnd(BigDecimal usedAmountVnd) {
        this.usedAmountVnd = usedAmountVnd;
    }
}