package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

public class SchoolSubscriptionQuotaRecord {
    private UUID id;
    private UUID schoolSubscriptionId;
    private QuotaType quotaType;
    private BigDecimal totalAllocatedAmountVnd;
    private BigDecimal usedAmountVnd;

    public SchoolSubscriptionQuotaRecord() {}

    public SchoolSubscriptionQuotaRecord(UUID id, UUID schoolSubscriptionId, QuotaType quotaType, BigDecimal totalAllocatedAmountVnd, BigDecimal usedAmountVnd) {
        this.id = id;
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
        this.usedAmountVnd = usedAmountVnd;
    }

    public SchoolSubscriptionQuotaRecord(UUID schoolSubscriptionId, QuotaType quotaType, BigDecimal totalAllocatedAmountVnd, BigDecimal usedAmountVnd) {
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
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

    public BigDecimal getTotalAllocatedAmountVnd() {
        return totalAllocatedAmountVnd;
    }

    public void setTotalAllocatedAmountVnd(BigDecimal totalAllocatedAmountVnd) {
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
    }

    public BigDecimal getUsedAmountVnd() {
        return usedAmountVnd;
    }

    public void setUsedAmountVnd(BigDecimal usedAmountVnd) {
        this.usedAmountVnd = usedAmountVnd;
    }
}