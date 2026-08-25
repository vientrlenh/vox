package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tagline")
    private String tagline;

    @Column(name = "price_vnd", nullable = false, precision = 18, scale = 0)
    private BigDecimal priceVnd;

    @Column(name = "period_type", nullable = false, updatable = false, check = {
        @CheckConstraint(
            name = "chk_subscription_plans_period_type_valid", 
            constraint = "period_type IN ('DAY', 'MONTH', 'YEAR')"
        )
    })
    private String periodType;

    @Column(name = "period_count", nullable = false)
    private Integer periodCount;

    @Column(name = "max_time_per_attempt_min", nullable = false)
    private Integer maxTimePerAttemptMin;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_subscription_plan_status_valid",
            constraint = "status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')"
        )
    })
    private String status;

    @Column(name = "version", nullable = false)
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "replaced_by_plan_id")
    private UUID replacedByPlanId;

    @Column(name = "service_fee_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal serviceFeeRatio;

    protected SubscriptionPlanJpaEntity() {}

    public SubscriptionPlanJpaEntity(UUID id, String name, String tagline, BigDecimal priceVnd, String periodType, Integer periodCount,
            Integer maxTimePerAttemptMin, String status, Long version,
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

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
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
