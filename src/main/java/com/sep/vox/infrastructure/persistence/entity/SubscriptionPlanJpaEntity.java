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

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "tagline", nullable = false, length = 2048)
    private String tagline;

    // VND không có đơn vị lẻ (hào/xu đã ngừng lưu hành) và PayOS/SePay chỉ nhận số nguyên -- giá gói
    // là tiền PHẢI THU nên để scale 0. Phải trùng orders.total_amount_vnd numeric(15,0): rộng hơn ở
    // đây thì Postgres LÀM TRÒN IM LẶNG lúc tạo đơn, gói niêm yết một giá mà thu một giá khác.
    @Column(name = "price_vnd", nullable = false, precision = 15, scale = 0)
    private BigDecimal priceVnd;

    @Column(name = "period_type", nullable = false, updatable = false, check = {
        @CheckConstraint(
            name = "chk_subscription_plans_period_type_valid", 
            constraint = "period_type IN ('DAY', 'MONTH', 'YEAR')"
        )
    })
    private String periodType;

    @Column(name = "period_count", nullable = false, check = {
        @CheckConstraint(
            name = "chk_subscription_plans_period_count_positive", 
            constraint = "period_count > 0"
        )
    })
    private Integer periodCount;

    @Column(name = "max_time_per_attempt_min", nullable = false, check = {
        @CheckConstraint(
            name = "chk_subscription_plans_max_time_per_attempt_min_positive", 
            constraint = "max_time_per_attempt_min > 0"
        )
    })
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

    protected SubscriptionPlanJpaEntity() {}

    public SubscriptionPlanJpaEntity(UUID id, String name, String tagline, BigDecimal priceVnd, String periodType, Integer periodCount,
            Integer maxTimePerAttemptMin, String status, Long version,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy, UUID replacedByPlanId) {
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

}
