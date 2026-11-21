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
@Table(name = "school_subscription_quota_records")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolSubscriptionQuotaRecordJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_subscription_id", nullable = false, updatable = false)
    private UUID schoolSubscriptionId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_records_quota_type_valid",
            constraint = "quota_type IN ('ASSESSMENT', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "total_allocated_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal totalAllocatedAmountVnd;

    @Column(name = "used_amount_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal usedAmountVnd;

    // The part of total that comes from the self-funded wallet instead of the plan -- see V12. It always grows TOGETHER with
    // total in one UPDATE statement (addFundingFromBalance), so the two columns cannot drift; the CHECK in the DB keeps
    // 0 <= funded <= total.
    @Column(name = "funded_from_balance_vnd", nullable = false, precision = 18, scale = 6, check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_records_funded_within_total",
            constraint = "funded_from_balance_vnd >= 0 AND funded_from_balance_vnd <= total_allocated_amount_vnd"
        )
    })
    private BigDecimal fundedFromBalanceVnd;

    // The promise "still has to receive the unspent self-funded money of the source period" -- see V13. NULL on almost every
    // row; only a period created by an EARLY renewal carries a value, and only until the next job run.
    @Column(name = "carry_funding_from_subscription_id", check = {
        @CheckConstraint(
            name = "chk_school_subscription_quota_records_carry_not_self",
            constraint = "carry_funding_from_subscription_id IS NULL"
                + " OR carry_funding_from_subscription_id <> school_subscription_id"
        )
    })
    private UUID carryFundingFromSubscriptionId;
}
