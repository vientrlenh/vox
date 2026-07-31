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
    private Integer maxStudentCount;
    private boolean popular;
    private PlanStatus status;
    private Integer version;
    private Instant createdAt;
    private UUID createdBy;

    public SubscriptionPlan() {}

    public SubscriptionPlan(UUID id, String name, String tagline, BigDecimal pricePerYear, Integer validityDays,
            Integer maxTimePerAttemptMin, Integer maxStudentCount, boolean popular, PlanStatus status, Integer version,
            Instant createdAt, UUID createdBy) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.pricePerYear = pricePerYear;
        this.validityDays = validityDays;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.maxStudentCount = maxStudentCount;
        this.popular = popular;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public SubscriptionPlan(String name, String tagline, BigDecimal pricePerYear, Integer validityDays,
            Integer maxTimePerAttemptMin, Integer maxStudentCount, boolean popular, PlanStatus status, Integer version,
            Instant createdAt, UUID createdBy) {
        this.name = name;
        this.tagline = tagline;
        this.pricePerYear = pricePerYear;
        this.validityDays = validityDays;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.maxStudentCount = maxStudentCount;
        this.popular = popular;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
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

    public Integer getMaxStudentCount() {
        return maxStudentCount;
    }

    public void setMaxStudentCount(Integer maxStudentCount) {
        this.maxStudentCount = maxStudentCount;
    }

    public boolean isPopular() {
        return popular;
    }

    public void setPopular(boolean popular) {
        this.popular = popular;
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
}
