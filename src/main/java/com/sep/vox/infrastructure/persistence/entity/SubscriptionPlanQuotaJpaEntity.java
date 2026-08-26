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

    // scale 6 chứ không phải 0 như tiền thu qua cổng: định mức được trừ dần theo từng lượt dùng, mà
    // một lượt luyện nói có thể chỉ tốn vài phần trăm đồng.
    @Column(name = "included_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal includedAmountVnd;

    protected SubscriptionPlanQuotaJpaEntity() {}

    public SubscriptionPlanQuotaJpaEntity(UUID id, UUID subscriptionPlanId, String quotaType, BigDecimal includedAmountVnd) {
        this.id = id;
        this.subscriptionPlanId = subscriptionPlanId;
        this.quotaType = quotaType;
        this.includedAmountVnd = includedAmountVnd;
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

    public BigDecimal getIncludedAmountVnd() {
        return includedAmountVnd;
    }

    public void setIncludedAmountVnd(BigDecimal includedAmountVnd) {
        this.includedAmountVnd = includedAmountVnd;
    }
}
