package com.sep.vox.infrastructure.persistence.entity.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The school's AI spending ledger -- see {@code SchoolAiSpendEntry} and V10 for why this table exists next to
 * {@code ai_usage_records} and {@code school_balance_entries}.
 *
 * <p>Every column is {@code updatable = false}: an append-only ledger, with no path for editing.
 */
@Entity
@Table(
    name = "school_ai_spend_entries",
    indexes = {
        @Index(name = "idx_school_ai_spend_entries_school_occurred", columnList = "school_id, occurred_at")
    },
    check = {
        @CheckConstraint(
            name = "chk_school_ai_spend_entries_quota_type_valid",
            constraint = "quota_type IN ('ASSESSMENT', 'PRACTICE')"
        ),
        @CheckConstraint(
            name = "chk_school_ai_spend_entries_amount_positive",
            constraint = "amount_vnd > 0"
        ),
        @CheckConstraint(
            name = "chk_school_ai_spend_entries_source_matches_quota_type",
            constraint = "(quota_type = 'ASSESSMENT' AND assessment_session_id IS NOT NULL AND practice_session_id IS NULL)"
                + " OR (quota_type = 'PRACTICE' AND practice_session_id IS NOT NULL AND assessment_session_id IS NULL)"
        )
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolAiSpendEntryJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20)
    private String quotaType;

    /** NULL = a charge for the whole school (a centralized exam), not missing data. */
    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "assessment_session_id", updatable = false)
    private UUID assessmentSessionId;

    @Column(name = "practice_session_id", updatable = false)
    private UUID practiceSessionId;

    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal amountVnd;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
