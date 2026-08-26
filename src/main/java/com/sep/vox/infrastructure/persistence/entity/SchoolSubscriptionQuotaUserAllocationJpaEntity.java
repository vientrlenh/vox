package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "school_subscription_quota_user_allocations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_school_subscription_quota_user_allocations_subscription_user",
            columnNames = {"school_subscription_id", "quota_type", "user_id"}
        )
    }
)
public class SchoolSubscriptionQuotaUserAllocationJpaEntity {

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

    @Column(name = "school_subscription_id", nullable = false, updatable = false)
    private UUID schoolSubscriptionId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_user_allocations_quota_type_valid",
            constraint = "quota_type IN ('EXAM', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "allocated_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal allocatedAmountVnd;

    @Column(name = "used_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal usedAmountVnd;

    protected SchoolSubscriptionQuotaUserAllocationJpaEntity() {}

    public SchoolSubscriptionQuotaUserAllocationJpaEntity(UUID id, UUID schoolSubscriptionId, String quotaType, UUID userId,
            BigDecimal allocatedAmountVnd, BigDecimal usedAmountVnd) {
        this.id = id;
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedAmountVnd = allocatedAmountVnd;
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

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
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
}