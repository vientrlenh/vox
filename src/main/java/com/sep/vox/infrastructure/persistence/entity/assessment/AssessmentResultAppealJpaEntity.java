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

@Entity
@Table(name = "assessment_result_appeals", indexes = {
    @Index(columnList = "candidate_result_id", name = "idx_assessment_result_appeals_candidate_result"),
    @Index(columnList = "status", name = "idx_assessment_result_appeals_status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentResultAppealJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_result_id", nullable = false, updatable = false)
    private UUID candidateResultId;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_result_appeals_status_valid",
            constraint = "status IN ('PENDING', 'APPROVED', 'GRADING', 'PUBLISHED', 'REJECTED', 'WITHDRAWN')"
        )
    })
    private String status;

    @Column(name = "score_before", precision = 5, scale = 2)
    private BigDecimal scoreBefore;

    @Column(name = "score_after", precision = 5, scale = 2)
    private BigDecimal scoreAfter;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "notes", length = 512)
    private String notes;

    /** Deadline to handle the appeal: set by the school admin at the approval step. */
    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "approved_at")
    private Instant approvedAt;

    /** Reason for rejection / note when publishing: kept apart from the student's `reason`. */
    @Column(name = "decision_note", length = 512)
    private String decisionNote;

    /** When the student withdrew the appeal themselves; the appeal attempt is given back. */
    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    /**
     * Reason an admin deliberately assigned the appeal to someone who HAS graded this result before. Required when
     * overriding the conflict-of-interest rule: a small school without enough teachers is real, but it must
     * leave a trace.
     */
    @Column(name = "reviewer_override_reason", columnDefinition = "TEXT")
    private String reviewerOverrideReason;
}
