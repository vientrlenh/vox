package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class LearnerProfile {

    private UUID id;
    private UUID studentId;
    private int version;
    private String goalType;
    private boolean autoUpdateInterest;
    private Instant quizCompletedAt;
    private Instant recordedAt;

    public LearnerProfile() {
    }

    public LearnerProfile(
            UUID id,
            UUID studentId,
            int version,
            String goalType,
            boolean autoUpdateInterest,
            Instant quizCompletedAt,
            Instant recordedAt) {
        this.id = id;
        this.studentId = studentId;
        this.version = version;
        this.goalType = goalType;
        this.autoUpdateInterest = autoUpdateInterest;
        this.quizCompletedAt = quizCompletedAt;
        this.recordedAt = recordedAt;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public static LearnerProfile first(UUID studentId) {
        return new LearnerProfile(
            null,
            studentId,
            1,
            null,
            true,
            null,
            Instant.now()
        );
    }

    public LearnerProfile next(
            String nextGoalType,
            Boolean nextAutoUpdate,
            Instant nextQuizCompletedAt) {
        return new LearnerProfile(
            null,
            studentId,
            version + 1,
            nextGoalType != null ? nextGoalType : goalType,
            nextAutoUpdate != null ? nextAutoUpdate : autoUpdateInterest,
            nextQuizCompletedAt != null
                ? nextQuizCompletedAt
                : quizCompletedAt,
            Instant.now()
        );
    }
}
