package com.sep.vox.infrastructure.persistence.entity.assessment;

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
 * An assessment session.
 *
 * <p>Soft delete does NOT use {@code @SQLRestriction}: the school administrator and the council chair must still read a
 * deleted session (to account for it when a student asks where the score went), but a restriction at the entity layer
 * hides it from them too. So {@code DELETED} is an ORDINARY status and each read path
 * decides for itself: the paths serving the administrator/chair let it through, the student's path and the other business flows
 * must exclude it.
 *
 * <p>Whoever writes a new query reading this table must ask: may the reader see a deleted session? See
 * {@code AssessmentSessionStatus#DELETED}.
 */
@Entity
@Table(name = "assessment_sessions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentSessionJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId; 

    @Column(name = "paper_id", nullable = false, updatable = false)
    private UUID paperId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_sessions_status_valid", 
            constraint = "status IN ('IN_PROGRESS', 'SUBMITTED', 'INTERRUPTED', 'GRADING', 'GRADED', 'EXPIRED', 'GRADING_FAILED', 'DELETED')"
        )
    }
    )
    private String status;

    @Column(name = "flagged", nullable = false)
    private boolean flagged;

    @Column(name = "flag_reason", columnDefinition = "text")
    private String flagReason;

    /** Goes with {@code status = 'DELETED'}: the CHECK in migration V8 keeps the two columns consistent. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Why it was deleted. Deleting an assessment attempt must be explainable to the student and parents. */
    @Column(name = "deleted_reason", columnDefinition = "text")
    private String deletedReason;

    @Column(name = "chosen_stream_type", length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_sessions_chosen_stream_type_valid",
            constraint = "chosen_stream_type IN ('CAMERA', 'SCREEN', 'CAMERA_AND_SCREEN')"
        )
    })
    private String chosenStreamType;

    @Column(name = "remaining_seconds")
    private Integer remainingSeconds;

    @Column(name = "grading_error", columnDefinition = "text")
    private String gradingError;

    @Column(name = "grading_retry_count")
    private Integer gradingRetryCount;

    // DEFAULT 0 must be present HERE and not only in V9: the persistence-layer tests run with
    // ddl-auto: create-drop, so their schema is built from this entity, not through Flyway. Without the default,
    // every native INSERT in a fixture (including existing fixtures unrelated to this
    // column) would have to list the new column itself -- an internal column should not make the whole test suite know about it.
    @Column(name = "school_regrade_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int schoolRegradeCount;
}
