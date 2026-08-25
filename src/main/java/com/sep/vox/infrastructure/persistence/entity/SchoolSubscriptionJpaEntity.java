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
@Table(name = "school_subscriptions")
public class SchoolSubscriptionJpaEntity {

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

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_plan_id", nullable = false, updatable = false)
    private UUID subscriptionPlanId;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_subscriptions_status_valid",
            constraint = "status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED')"
        )
    })
    private String status;

    // Số tiền trường ĐÃ TRẢ thật -- cùng nhóm "tiền phải thu" với subscription_plans.price_vnd và
    // orders.total_amount_vnd, nên dùng chung numeric(15,0).
    @Column(name = "price_paid_snapshot", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal pricePaidSnapshot;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason")
    private String suspendedReason;

    @Column(name = "suspended_by")
    private UUID suspendedBy;

    protected SchoolSubscriptionJpaEntity() {}

    public SchoolSubscriptionJpaEntity(UUID id, UUID schoolId, UUID subscriptionPlanId, Instant startDate, Instant endDate,
            String status, BigDecimal pricePaidSnapshot, Instant cancelledAt, Instant createdAt, Long version,
            Instant suspendedAt, String suspendedReason, UUID suspendedBy) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionPlanId = subscriptionPlanId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.pricePaidSnapshot = pricePaidSnapshot;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.version = version;
        this.suspendedAt = suspendedAt;
        this.suspendedReason = suspendedReason;
        this.suspendedBy = suspendedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPricePaidSnapshot() {
        return pricePaidSnapshot;
    }

    public void setPricePaidSnapshot(BigDecimal pricePaidSnapshot) {
        this.pricePaidSnapshot = pricePaidSnapshot;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(Instant suspendedAt) {
        this.suspendedAt = suspendedAt;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public void setSuspendedReason(String suspendedReason) {
        this.suspendedReason = suspendedReason;
    }

    public UUID getSuspendedBy() {
        return suspendedBy;
    }

    public void setSuspendedBy(UUID suspendedBy) {
        this.suspendedBy = suspendedBy;
    }
}
