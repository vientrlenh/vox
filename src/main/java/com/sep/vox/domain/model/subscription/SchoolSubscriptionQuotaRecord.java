package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

public class SchoolSubscriptionQuotaRecord {
    private UUID id;
    private UUID schoolSubscriptionId;
    private QuotaType quotaType;
    private BigDecimal totalAllocated;
    private BigDecimal usedQuantity;

    public SchoolSubscriptionQuotaRecord() {}

    public SchoolSubscriptionQuotaRecord(UUID id, UUID schoolSubscriptionId, QuotaType quotaType, BigDecimal totalAllocated, BigDecimal usedQuantity) {
        this.id = id;
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.totalAllocated = totalAllocated;
        this.usedQuantity = usedQuantity;
    }

    public SchoolSubscriptionQuotaRecord(UUID schoolSubscriptionId, QuotaType quotaType, BigDecimal totalAllocated, BigDecimal usedQuantity) {
        this.schoolSubscriptionId = schoolSubscriptionId;
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