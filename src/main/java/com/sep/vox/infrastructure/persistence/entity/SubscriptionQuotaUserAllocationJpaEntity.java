package com.sep.vox.infrastructure.persistence.entity;

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
    name = "subscription_quota_user_allocations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_subscription_quota_user_allocations_subscription_quota_user",
            columnNames = {"subscription_id", "quota_type", "user_id"}
        )
    }
)
public class SubscriptionQuotaUserAllocationJpaEntity {

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
            name = "chk_subscription_quota_user_allocations_quota_type_valid",
            constraint = "quota_type IN ('CLASS_TEST', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedQuantity;

    @Column(name = "used_quantity", nullable = false)
    private Integer usedQuantity;

    protected SubscriptionQuotaUserAllocationJpaEntity() {}

    public SubscriptionQuotaUserAllocationJpaEntity(UUID id, UUID subscriptionId, String quotaType, UUID userId,
            Integer allocatedQuantity, Integer usedQuantity) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.allocatedQuantity = allocatedQuantity;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(Integer allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }

    public Integer getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(Integer usedQuantity) {
        this.usedQuantity = usedQuantity;
    }
}
