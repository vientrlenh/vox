package com.sep.vox.infrastructure.persistence.entity.assessment;

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
 * The status-change log of an assessment attempt. An append-only table: no updated_* columns, and no
 * use case edits or deletes a row once written.
 *
 * <p>There is no CHECK on {@code from_status}/{@code to_status}: the values come from
 * {@code AssessmentCandidateResultStatus}, that enum still changes, and {@code ddl-auto}
 * cannot alter a CHECK constraint -- a hard constraint here would become technical debt.
 */
@Entity
@Table(name = "assessment_result_status_histories", indexes = {
    @Index(columnList = "candidate_result_id, created_at", name = "idx_result_status_histories_result_time")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentResultStatusHistoryJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_result_id", nullable = false, updatable = false)
    private UUID candidateResultId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "score_before", precision = 5, scale = 2)
    private BigDecimal scoreBefore;

    @Column(name = "score_after", precision = 5, scale = 2)
    private BigDecimal scoreAfter;

    @Column(name = "source", nullable = false, length = 30, check = {
        @CheckConstraint(
            name = "chk_assessment_result_status_histories_source_valid",
            constraint = "source IN ('AI_EVALUATION', 'TEACHER_INITIAL', 'TEACHER_SPOT_CHECK', "
                + "'TEACHER_REMEDIATION', 'TEACHER_APPEAL', 'ADMIN_BULK_FINALIZE', 'ASSESSMENT_PUBLISH', 'SYSTEM')"
        )
    })
    private String source;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
