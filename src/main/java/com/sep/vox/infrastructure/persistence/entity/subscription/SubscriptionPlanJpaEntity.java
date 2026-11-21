package com.sep.vox.infrastructure.persistence.entity.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPlanJpaEntity extends BaseEntity {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "tagline", nullable = false, length = 2048)
    private String tagline;

    // VND has no fractional unit (hao/xu are no longer in circulation) and PayOS/SePay only accept integers -- the plan
    // price is money TO COLLECT, so scale 0. It must match orders.total_amount_vnd numeric(15,0): if it were wider here,
    // Postgres would ROUND SILENTLY when the order is created, listing one price on the plan and collecting another.
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

    @Column(name = "replaced_by_plan_id")
    private UUID replacedByPlanId;
}
