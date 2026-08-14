package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SubscriptionPlan {
    private UUID id;
    private String name;
    private String tagline;
    private BigDecimal pricePerYear;
    private Integer validityDays;
    private Integer maxTimePerAttemptMin;
    private PlanStatus status;
    private Integer version;
    private Instant createdAt;
    private UUID createdBy;
    private UUID replacedByPlanId;
    private BigDecimal serviceFeeRatio;

    public SubscriptionPlan() {}

    public SubscriptionPlan(UUID id, String name, String tagline, BigDecimal pricePerYear, Integer validityDays,
            Integer maxTimePerAttemptMin, PlanStatus status, Integer version,
            Instant createdAt, UUID createdBy, UUID replacedByPlanId, BigDecimal serviceFeeRatio) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.pricePerYear = pricePerYear;
        this.validityDays = validityDays;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.replacedByPlanId = replacedByPlanId;
        this.serviceFeeRatio = serviceFeeRatio;
    }

    public SubscriptionPlan(String name, String tagline, BigDecimal pricePerYear, Integer validityDays,
            Integer maxTimePerAttemptMin, PlanStatus status, Integer version,
            Instant createdAt, UUID createdBy, BigDecimal serviceFeeRatio) {
        this.name = name;
        this.tagline = tagline;
        this.pricePerYear = pricePerYear;
        this.validityDays = validityDays;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
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

    public BigDecimal getPricePerYear() {
        return pricePerYear;
    }

    public void setPricePerYear(BigDecimal pricePerYear) {
        this.pricePerYear = pricePerYear;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public Integer getMaxTimePerAttemptMin() {
        return maxTimePerAttemptMin;
    }

    public void setMaxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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
