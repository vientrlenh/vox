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
@Table(name = "subscription_plan_quotas")
public class SubscriptionPlanQuotaJpaEntity {

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

    @Column(name = "subscription_plan_id", nullable = false, updatable = false)
    private UUID subscriptionPlanId;

    @Column(name = "quota_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_subscription_plan_quotas_quota_type_valid",
            constraint = "quota_type IN ('GRADING', 'CLASS_TEST', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "included_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal includedQuantity;

    @Column(name = "token_unit_price_vnd", nullable = false, precision = 15, scale = 0)
    private BigDecimal tokenUnitPriceVnd;

    protected SubscriptionPlanQuotaJpaEntity() {}

    public SubscriptionPlanQuotaJpaEntity(UUID id, UUID subscriptionPlanId, String quotaType, BigDecimal includedQuantity, BigDecimal tokenUnitPriceVnd) {
        this.id = id;
        this.subscriptionPlanId = subscriptionPlanId;
        this.quotaType = quotaType;
        this.includedQuantity = includedQuantity;
        this.tokenUnitPriceVnd = tokenUnitPriceVnd;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getIncludedQuantity() {
        return includedQuantity;
    }

    public void setIncludedQuantity(BigDecimal includedQuantity) {
        this.includedQuantity = includedQuantity;
    }

    public BigDecimal getTokenUnitPriceVnd() {
        return tokenUnitPriceVnd;
    }

    public void setTokenUnitPriceVnd(BigDecimal tokenUnitPriceVnd) {
        this.tokenUnitPriceVnd = tokenUnitPriceVnd;
    }
}
