package com.sep.vox.infrastructure.persistence.entity.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolSubscriptionQuotaUserAllocationJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_subscription_id", nullable = false, updatable = false)
    private UUID schoolSubscriptionId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_user_allocations_quota_type_valid",
            constraint = "quota_type IN ('ASSESSMENT', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "allocated_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal allocatedAmountVnd;

    @Column(name = "used_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal usedAmountVnd;
}
