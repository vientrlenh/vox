package com.sep.vox.infrastructure.persistence.entity.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "subscription_plan_quotas")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPlanQuotaJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "subscription_plan_id", nullable = false, updatable = false)
    private UUID subscriptionPlanId;

    @Column(name = "quota_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_subscription_plan_quotas_quota_type_valid",
            constraint = "quota_type IN ('ASSESSMENT', 'PRACTICE')"
        )
    })
    private String quotaType;

    // scale 6, not 0 like money collected through the gateway: the quota is deducted bit by bit on every usage, and one
    // speaking-practice turn may cost only a few hundredths of a dong.
    @Column(name = "included_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal includedAmountVnd;
}
