package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

/**
 * Learner profile -- ONE row per student, updated in place.
 *
 * <p>The previous design was append-only with a {@code version} column: every goal change / quiz resubmission / auto-update
 * toggle produced a new row. It was dropped because that history was written but nobody read it -- details are in the
 * header comment of V18__learner_profile_single_row.sql (the migration that dropped the {@code version} column).
 *
 * <p>{@code first()} and {@code next()} were removed with it: they only existed to build the next version. Plain setters
 * are used now, and {@code applyChanges} gathers the "overwrite only when a value is given" part.
 */
public class LearnerProfile {

    private UUID id;
    private UUID studentId;
    private String goalType;
    private boolean autoUpdateInterest;
    private Instant quizCompletedAt;
    private Instant recordedAt;

    public LearnerProfile() {
    }

    /** The default profile for a student who has nothing yet -- auto-update of interests is on. */
    public static LearnerProfile forStudent(UUID studentId) {
        return LearnerProfile.builder()
                .id(null)
                .studentId(studentId)
                .goalType(null)
                .autoUpdateInterest(true)
                .quizCompletedAt(null)
                .recordedAt(Instant.now())
                .build();
    }

    /**
     * Overwrites the fields that have a value and keeps the rest.
     *
     * <p>{@code null} means "do not touch", exactly the semantics of the old {@code next()}: each
     * action (set a goal / submit a quiz / toggle) passes only the part it is responsible for.
     */
    public void applyChanges(
            String nextGoalType,
            Boolean nextAutoUpdate,
            Instant nextQuizCompletedAt) {
        if (nextGoalType != null) {
            this.goalType = nextGoalType;
        }
        if (nextAutoUpdate != null) {
            this.autoUpdateInterest = nextAutoUpdate;
        }
        if (nextQuizCompletedAt != null) {
            this.quizCompletedAt = nextQuizCompletedAt;
        }
        this.recordedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public String getGoalType() {
        return goalType;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public boolean isAutoUpdateInterest() {
        return autoUpdateInterest;
    }

    public void setAutoUpdateInterest(boolean autoUpdateInterest) {
        this.autoUpdateInterest = autoUpdateInterest;
    }

    public Instant getQuizCompletedAt() {
        return quizCompletedAt;
    }

    public void setQuizCompletedAt(Instant quizCompletedAt) {
        this.quizCompletedAt = quizCompletedAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final LearnerProfile target = new LearnerProfile();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder studentId(UUID studentId) {
            target.studentId = studentId;
            return this;
        }

        public Builder goalType(String goalType) {
            target.goalType = goalType;
            return this;
        }

        public Builder autoUpdateInterest(boolean autoUpdateInterest) {
            target.autoUpdateInterest = autoUpdateInterest;
            return this;
        }

        public Builder quizCompletedAt(Instant quizCompletedAt) {
            target.quizCompletedAt = quizCompletedAt;
            return this;
        }

        public Builder recordedAt(Instant recordedAt) {
            target.recordedAt = recordedAt;
            return this;
        }

        public LearnerProfile build() {
            return target;
        }

    }
}
