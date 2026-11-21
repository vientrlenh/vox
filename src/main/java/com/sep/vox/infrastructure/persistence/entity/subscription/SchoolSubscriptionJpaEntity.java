package com.sep.vox.infrastructure.persistence.entity.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "school_subscriptions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolSubscriptionJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
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

    // The amount the school ACTUALLY PAID -- in the same "money to collect" group as subscription_plans.price_vnd and
    // orders.total_amount_vnd, so it shares numeric(15,0).
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
}
