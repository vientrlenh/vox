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

    public static SchoolSubscriptionQuotaUserAllocation unallocated(
            UUID schoolSubscriptionId, QuotaType quotaType, UUID userId) {
        return builder()
                .schoolSubscriptionId(schoolSubscriptionId)
                .quotaType(quotaType)
                .userId(userId)
                .allocatedAmountVnd(BigDecimal.ZERO)
                .usedAmountVnd(BigDecimal.ZERO)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolSubscriptionQuotaUserAllocation target = new SchoolSubscriptionQuotaUserAllocation();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder schoolSubscriptionId(UUID schoolSubscriptionId) {
            target.schoolSubscriptionId = schoolSubscriptionId;
            return this;
        }

        public Builder quotaType(QuotaType quotaType) {
            target.quotaType = quotaType;
            return this;
        }

        public Builder userId(UUID userId) {
            target.userId = userId;
            return this;
        }

        public Builder allocatedAmountVnd(BigDecimal allocatedAmountVnd) {
            target.allocatedAmountVnd = allocatedAmountVnd;
            return this;
        }

        public Builder usedAmountVnd(BigDecimal usedAmountVnd) {
            target.usedAmountVnd = usedAmountVnd;
            return this;
        }

        public SchoolSubscriptionQuotaUserAllocation build() {
            return target;
        }

    }
}
