package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticeGradingStatus;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "practice_item_responses",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_response_session_question",
        columnNames = {"practice_session_id", "practice_question_id"}
    ),
    indexes = @Index(name = "idx_practice_response_session", columnList = "practice_session_id")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeItemResponseJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_session_id", nullable = false, updatable = false)
    private UUID practiceSessionId;
    @Column(name = "practice_question_id", nullable = false, updatable = false)
    private UUID practiceQuestionId;
    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;
    /** Whether the follow-up chain of this question has ended -- see V14 for why it has to be stored. */
    @Column(name = "question_complete", nullable = false)
    private boolean questionComplete;

    /**
     * The grading lifecycle -- values follow {@link com.sep.vox.domain.model.personalization.PracticeGradingStatus}.
     *
     * <p>Kept as a String with a {@code @CheckConstraint} rather than {@code @Enumerated}, following exactly the mold of
     * {@code AssessmentSessionJpaEntity.status}: the constraint lives in the DB so junk data cannot get in even
     * if someone edits by hand, while the enum takes care of type safety at the application layer.
     */
    @Column(name = "grading_status", nullable = false, length = 16, check = {
        @CheckConstraint(
            name = "chk_practice_item_response_grading_status",
            constraint = "grading_status IN ('PENDING', 'GRADING', 'GRADED', 'GRADING_FAILED')"
        )
    })
    // INITIALISE IT RIGHT HERE, do not rely on the Postgres DEFAULT.
    //
    // V21 declares the column as NOT NULL DEFAULT 'PENDING', but a DEFAULT only applies when the INSERT statement does NOT
    // mention the column. Once the column is mapped on the entity, Hibernate always puts it into the INSERT with the
    // Java value -- if nothing assigns it, that value is null, and Postgres rejects it.
    //
    // Measured on production 2026-08-13: every POST /internal/practice-sessions/{id}/turns returned 500, all three
    // attempts failed, the agents closed the connection -- a student who was practising got thrown out midway and the session
    // was recorded as ABANDONED/TOO_HARD even though they had spoken.
    @Builder.Default
    private String gradingStatus = PracticeGradingStatus.PENDING.name();

    /**
     * The time the latest grading request was sent. It goes IN PAIR with {@link #gradingStatus}: the status says where it
     * is, this timestamp says how long it has been there -- without it a GRADING row hangs forever and nobody rescues it.
     */
    @Column(name = "grading_requested_at")
    private Instant gradingRequestedAt;

    /** The number of times a grading request was sent. Capped by {@code PracticeGradingFlushService.MAX_GRADING_ATTEMPTS}. */
    @Column(name = "grading_attempts", nullable = false)
    private int gradingAttempts;
}
