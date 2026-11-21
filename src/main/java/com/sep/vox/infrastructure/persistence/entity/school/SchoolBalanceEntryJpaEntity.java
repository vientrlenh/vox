package com.sep.vox.infrastructure.persistence.entity.school;

import java.math.BigDecimal;
import java.time.Instant;
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

/**
 * Append-only: every column is {@code updatable = false}. Editing an entry that has been written is wrong business-wise
 * -- to adjust, write an additional ADJUSTMENT row.
 */
@Entity
@Table(name = "school_balance_entries")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolBalanceEntryJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id", updatable = false)
    private UUID subscriptionId;

    @Column(name = "entry_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_balance_entries_entry_type_valid",
            constraint = "entry_type IN ('TOP_UP', 'OVERAGE_CHARGE', 'QUOTA_FUNDING', 'REFUND', 'ADJUSTMENT')"
        )
    })
    private String entryType;

    // numeric(18,6), not (15,0) like cash through the gateway: one speaking-practice turn may cost only a few hundredths of
    // a dong, and rounding to an integer would wipe out that deduction.
    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal amountVnd;

    @Column(name = "balance_after_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal balanceAfterVnd;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "assessment_session_id", updatable = false)
    private UUID assessmentSessionId;

    // A SEPARATE column rather than sharing assessment_session_id for both kinds of session: each column here is a real foreign
    // key (fk_..._assessment_session -> assessment_sessions), so putting a practice session id into the assessment
    // column would violate the FK. This is also exactly the "TYPED column" choice that this table deliberately used instead of the polymorphic pair
    // (source_type, source_id) like the old invoice -- see the javadoc of SchoolBalanceEntry.
    //
    // The constraint chk_school_balance_entries_overage_traceable must require EXACTLY ONE of the two columns to be set
    // for OVERAGE_CHARGE (num_nonnulls(assessment_session_id, practice_session_id) = 1), not just
    // assessment_session_id NOT NULL as before.
    @Column(name = "practice_session_id", updatable = false)
    private UUID practiceSessionId;

    @Column(name = "quota_type", updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_balance_entries_quota_type_valid",
            constraint = "quota_type IS NULL OR quota_type IN ('ASSESSMENT', 'PRACTICE')"
        )
    })
    private String quotaType;

    // Kept in USD: this is the bill the provider charges us, reconciled back against ai_usage_records and the input of
    // QuotaPricingCalibrationService. Converting to VND here would make the calibrated rate drift with the exchange rate
    // instead of with the real cost.
    @Column(name = "cost_usd", updatable = false, precision = 18, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "fx_rate_used", updatable = false, precision = 12, scale = 4)
    private BigDecimal fxRateUsed;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "reason", updatable = false, length = 2048)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
