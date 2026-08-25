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

@Entity
@Table(name = "school_subscription_quota_records")
public class SchoolSubscriptionQuotaRecordJpaEntity {

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

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_records_quota_type_valid",
            constraint = "quota_type IN ('GRADING', 'CLASS_TEST', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "total_allocated", nullable = false, precision = 18, scale = 6)
    private BigDecimal totalAllocated;

    @Column(name = "used_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal usedQuantity;

    protected SchoolSubscriptionQuotaRecordJpaEntity() {}

    public SchoolSubscriptionQuotaRecordJpaEntity(UUID id, UUID subscriptionId, String quotaType, BigDecimal totalAllocated, BigDecimal usedQuantity) {
        this.id = id;
        this.subscriptionId = subscriptionId;
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

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
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