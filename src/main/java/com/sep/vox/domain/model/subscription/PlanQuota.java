package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

public class PlanQuota {
    private UUID id;
    private UUID planId;
    private QuotaType quotaType;
    private Integer includedQuantity;
    private BigDecimal tokenUnitPrice;

    public PlanQuota() {}

    public PlanQuota(UUID id, UUID planId, QuotaType quotaType, Integer includedQuantity, BigDecimal tokenUnitPrice) {
        this.id = id;
        this.planId = planId;
        this.quotaType = quotaType;
        this.includedQuantity = includedQuantity;
        this.tokenUnitPrice = tokenUnitPrice;
    }

    public PlanQuota(UUID planId, QuotaType quotaType, Integer includedQuantity, BigDecimal tokenUnitPrice) {
        this.planId = planId;
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

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public Integer getIncludedQuantity() {
        return includedQuantity;
    }

    public void setIncludedQuantity(Integer includedQuantity) {
        this.includedQuantity = includedQuantity;
    }

    public BigDecimal getTokenUnitPrice() {
        return tokenUnitPrice;
    }

    public void setTokenUnitPrice(BigDecimal tokenUnitPrice) {
        this.tokenUnitPrice = tokenUnitPrice;
    }
}
