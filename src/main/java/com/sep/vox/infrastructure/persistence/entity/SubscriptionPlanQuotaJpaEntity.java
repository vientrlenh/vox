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

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "quota_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_subscription_plan_quotas_quota_type_valid",
            constraint = "quota_type IN ('GRADING', 'CLASS_TEST', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "included_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal includedQuantity;

    @Column(name = "token_unit_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal tokenUnitPrice;

    protected SubscriptionPlanQuotaJpaEntity() {}

    public SubscriptionPlanQuotaJpaEntity(UUID id, UUID planId, String quotaType, BigDecimal includedQuantity, BigDecimal tokenUnitPrice) {
        this.id = id;
        this.planId = planId;
        this.quotaType = quotaType;
        this.includedQuantity = includedQuantity;
        this.tokenUnitPrice = tokenUnitPrice;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
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

    public BigDecimal getTokenUnitPrice() {
        return tokenUnitPrice;
    }

    public void setTokenUnitPrice(BigDecimal tokenUnitPrice) {
        this.tokenUnitPrice = tokenUnitPrice;
    }
}
