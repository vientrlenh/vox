package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class LearnerProfile {

    private UUID id;
    private UUID studentId;
    private int version;
    private String goalType;
    private String targetExam;
    private LocalDate targetDate;
    private BigDecimal flsaScore;
    private String flsaRawAnswersJson;
    private boolean autoUpdateInterest;
    private OffsetDateTime quizCompletedAt;
    private OffsetDateTime recordedAt;

    public LearnerProfile() {
    }

    public LearnerProfile(
            UUID id,
            UUID studentId,
            int version,
            String goalType,
            String targetExam,
            LocalDate targetDate,
            BigDecimal flsaScore,
            String flsaRawAnswersJson,
            boolean autoUpdateInterest,
            OffsetDateTime quizCompletedAt,
            OffsetDateTime recordedAt) {
        this.id = id;
        this.studentId = studentId;
        this.version = version;
        this.goalType = goalType;
        this.targetExam = targetExam;
        this.targetDate = targetDate;
        this.flsaScore = flsaScore;
        this.flsaRawAnswersJson = flsaRawAnswersJson;
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

    public String getTargetExam() {
        return targetExam;
    }

    public void setTargetExam(String targetExam) {
        this.targetExam = targetExam;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public BigDecimal getFlsaScore() {
        return flsaScore;
    }

    public void setFlsaScore(BigDecimal flsaScore) {
        this.flsaScore = flsaScore;
    }

    public String getFlsaRawAnswersJson() {
        return flsaRawAnswersJson;
    }

    public void setFlsaRawAnswersJson(String flsaRawAnswersJson) {
        this.flsaRawAnswersJson = flsaRawAnswersJson;
    }

    public boolean isAutoUpdateInterest() {
        return autoUpdateInterest;
    }

    public void setAutoUpdateInterest(boolean autoUpdateInterest) {
        this.autoUpdateInterest = autoUpdateInterest;
    }

    public OffsetDateTime getQuizCompletedAt() {
        return quizCompletedAt;
    }

    public void setQuizCompletedAt(OffsetDateTime quizCompletedAt) {
        this.quizCompletedAt = quizCompletedAt;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public static LearnerProfile first(UUID studentId) {
        return new LearnerProfile(
            null,
            studentId,
            1,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            OffsetDateTime.now()
        );
    }

    public LearnerProfile next(
            String nextGoalType,
            BigDecimal nextFlsaScore,
            String nextFlsaRawAnswersJson,
            Boolean nextAutoUpdate,
            OffsetDateTime nextQuizCompletedAt) {
        return new LearnerProfile(
            null,
            studentId,
            version + 1,
            nextGoalType != null ? nextGoalType : goalType,
            targetExam,
            targetDate,
            nextFlsaScore != null ? nextFlsaScore : flsaScore,
            nextFlsaRawAnswersJson != null
                ? nextFlsaRawAnswersJson
                : flsaRawAnswersJson,
            nextAutoUpdate != null ? nextAutoUpdate : autoUpdateInterest,
            nextQuizCompletedAt != null
                ? nextQuizCompletedAt
                : quizCompletedAt,
            OffsetDateTime.now()
        );
    }
}
