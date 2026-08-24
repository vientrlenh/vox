package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SubscriptionPlan {
    private UUID id;
    private String name;
    private String tagline;
    private BigDecimal priceVnd;
    private SubscriptionPlanPeriod periodType;
    private Integer periodCount;
    private Integer maxTimePerAttemptMin;
    private SubscriptionPlanStatus status;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private UUID replacedByPlanId;
    private BigDecimal serviceFeeRatio;

    public SubscriptionPlan() {}

    public SubscriptionPlan(UUID id, String name, String tagline, BigDecimal priceVnd, SubscriptionPlanPeriod periodType, Integer periodCount,
            Integer maxTimePerAttemptMin, SubscriptionPlanStatus status, Integer version,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy, UUID replacedByPlanId, BigDecimal serviceFeeRatio) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.priceVnd = priceVnd;
        this.periodType = periodType;
        this.periodCount = periodCount;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt; 
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.replacedByPlanId = replacedByPlanId;
        this.serviceFeeRatio = serviceFeeRatio;
    }

    public SubscriptionPlan(String name, String tagline, BigDecimal priceVnd, SubscriptionPlanPeriod periodType, Integer periodCount,
            Integer maxTimePerAttemptMin, SubscriptionPlanStatus status, Integer version,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy, BigDecimal serviceFeeRatio) {
        this.name = name;
        this.tagline = tagline;
        this.priceVnd = priceVnd;
        this.periodType = periodType;
        this.periodCount = periodCount;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.serviceFeeRatio = serviceFeeRatio;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public BigDecimal getPriceVnd() {
        return priceVnd;
    }

    public void setPriceVnd(BigDecimal priceVnd) {
        this.priceVnd = priceVnd;
    }

    public SubscriptionPlanPeriod getPeriodType() {
        return periodType;
    }

    public void setPeriodType(SubscriptionPlanPeriod periodType) {
        this.periodType = periodType;
    }

    public Integer getPeriodCount() {
        return periodCount;
    }

    public void setPeriodCount(Integer periodCount) {
        this.periodCount = periodCount;
    }

    public Integer getMaxTimePerAttemptMin() {
        return maxTimePerAttemptMin;
    }

    public void setMaxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
    }

    public SubscriptionPlanStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionPlanStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public UUID getReplacedByPlanId() {
        return replacedByPlanId;
    }

    public void setReplacedByPlanId(UUID replacedByPlanId) {
        this.replacedByPlanId = replacedByPlanId;
    }

    public BigDecimal getServiceFeeRatio() {
        return serviceFeeRatio;
    }

    public void setServiceFeeRatio(BigDecimal serviceFeeRatio) {
        this.serviceFeeRatio = serviceFeeRatio;
    }

}
