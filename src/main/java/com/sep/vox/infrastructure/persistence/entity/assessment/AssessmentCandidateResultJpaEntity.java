package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_candidate_results")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentCandidateResultJpaEntity extends BaseEntity {

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "assessment_policy_id", nullable = false, updatable = false)
    private UUID assessmentPolicyId;

    @Column(name = "policy_version", nullable = false, updatable = false)
    private int policyVersion;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "target_framework_band_id", nullable = false, updatable = false)
    private UUID targetFrameworkBandId;

    // Nullable: a session that was kicked (blockedAt) BEFORE it was ever graded by the AI has no
    // band/score to assign (item B.2) - the INVALID result then has no real band/score.
    @Column(name = "rubric_result_band_id", nullable = true)
    private UUID rubricResultBandId;

    @Column(name = "total_score", nullable = true, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_candidate_results_status_valid",
            constraint = "status IN ('PENDING_REVIEW', 'RELEASED', 'APPEALED', 'RE_GRADING', 'FINAL', 'INVALID', 'RETAKE_REQUIRED', 'PASSED', 'FAILED', 'DELETED')"
        )
    })
    private String status;

    /** Goes with {@code status = 'DELETED'}: the CHECK in migration V8 keeps the two columns consistent. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Copied from the session when it was deleted, so the reason can be read from the result without a reverse join. */
    @Column(name = "deleted_reason", columnDefinition = "text")
    private String deletedReason;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;
}
