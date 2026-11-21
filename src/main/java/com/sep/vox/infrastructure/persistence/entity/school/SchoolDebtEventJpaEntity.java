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

@Entity
@Table(
    name = "school_debt_events",
    // Restates the V4 constraint verbatim, the same way the two @CheckConstraint below repeat V1/V2. It MUST be repeated:
    // the test profile runs ddl-auto=create-drop, so the schema the tests face is built by Hibernate from these very
    // annotations, NOT by Flyway from the migrations. A constraint written only in the .sql file would not exist when the
    // tests run, and the most important guard of this table would have no test line touching it.
    check = @CheckConstraint(
        name = "chk_school_debt_events_shape_matches_event_type",
        constraint = "(event_type <> 'CLEARED'"
            + " AND quota_type IS NOT NULL"
            + " AND total_allocated_vnd IS NOT NULL"
            + " AND used_amount_vnd IS NOT NULL"
            + " AND num_nonnulls(trigger_assessment_session_id, trigger_practice_session_id) = 1)"
            + " OR (event_type = 'CLEARED'"
            + " AND quota_type IS NULL"
            + " AND total_allocated_vnd IS NULL"
            + " AND used_amount_vnd IS NULL"
            + " AND trigger_assessment_session_id IS NULL"
            + " AND trigger_practice_session_id IS NULL"
            + " AND overage_vnd = 0)"
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolDebtEventJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_debt_event_event_type_valid",
            constraint = "event_type IN ('LOCKED', 'CAP_EXCEEDED', 'CLEARED')"
        )
    })
    private String eventType;

    // nullable since V4: CLEARED is a SCHOOL-level event, the balance is not split by any quota wallet, so "which wallet just
    // cleared its debt" has no answer. chk_school_debt_events_shape_matches_event_type enforces the exact column combination
    // for each event type.
    @Column(name = "quota_type", updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_debt_events_quota_type_valid",
            constraint = "quota_type IN ('ASSESSMENT', 'PRACTICE')"
        )
    })
    private String quotaType;

    @Column(name = "trigger_assessment_session_id", updatable = false)
    private UUID triggerAssessmentSessionId;

    @Column(name = "trigger_practice_session_id", updatable = false)
    private UUID triggerPracticeSessionId;

    @Column(name = "trigger_amount_vnd", updatable = false, precision = 18, scale = 6)
    private BigDecimal triggerAmountVnd;

    @Column(name = "total_allocated_vnd", updatable = false, precision = 18, scale = 6)
    private BigDecimal totalAllocatedVnd;

    @Column(name = "used_amount_vnd", updatable = false, precision = 18, scale = 6)
    private BigDecimal usedAmountVnd;

    @Column(name = "overage_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal overageVnd;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
