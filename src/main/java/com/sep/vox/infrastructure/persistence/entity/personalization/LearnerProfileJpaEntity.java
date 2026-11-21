package com.sep.vox.infrastructure.persistence.entity.personalization;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "learner_profiles",
    // Must match V16: the index is now only on student_id (a 1-1 invariant). Declaring the `version` column
    // here would tell Hibernate to create an index on a column that does not exist.
    indexes = @Index(
        name = "idx_learner_profile_student",
        columnList = "student_id",
        unique = true
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearnerProfileJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "goal_type", length = 24)
    private String goalType;

    // There is no `version` column any more: one profile per student, updated in place. V16 dropped this column from
    // the DB -- see the header comment of that migration.

    // REMOVED 2026-08-07: flsaScore / flsaRawAnswersJson -- the foreign language anxiety self-assessment scale.
    // The two columns `flsa_score` / `flsa_raw_answers_json` are also no longer in the DB -- V14 dropped
    // them (together with target_assessment / target_date) from the table created by V13.
    // They WERE written (through the submitFlsaSelfReport mutation) but were never READ to change
    // behavior: no question generation, difficulty selection or topic ranking flow looks them up. The client also
    // dropped the questionnaire screen earlier -- Flutter only keeps a note reminding that the mutation still exists in case it needs to come back.

    @Column(name = "auto_update_interest", nullable = false)
    @Builder.Default
    private boolean autoUpdateInterest = true;

    @Column(name = "quiz_completed_at")
    private Instant quizCompletedAt;

    // updatable: the profile is now updated IN PLACE, so this timestamp must change with it. The old version locked
    // updatable=false because every change produced a new row and the timestamp never needed editing.
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
