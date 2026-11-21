package com.sep.vox.infrastructure.persistence.entity.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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

/**
 * The school's SELF-FUNDED money wallet. The quota that comes with the plan does NOT live here but in
 * school_subscription_quota_records -- split per QuotaType, so merging it into a single school-level balance would erase
 * the per-type limit.
 */
@Entity
@Table(name = "school_balances")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolBalanceJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    // Money the school TOPPED UP itself: it does not expire. There is DELIBERATELY no CHECK >= 0 -- the negative part here IS
    // the debt, replacing the old used_quantity > total_allocated condition. Debt is a matter for the SCHOOL, no longer split
    // by QuotaType -- see SchoolBalance / SchoolSubscriptionDebtGuardService.
    @Column(name = "balance_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal balanceVnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // The whole school piles onto EXACTLY ONE row here, so every balance deduction contends with the others. @Version is only
    // the last line of defence; what really serializes the writes is the row lock of
    // SchoolBalanceRepository.findBySchoolIdForUpdateOrCreate -- there is DELIBERATELY no in-place UPDATE ... WHERE style
    // deduction, because the accompanying entry needs balance_after_vnd, which a single statement like that cannot return.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
