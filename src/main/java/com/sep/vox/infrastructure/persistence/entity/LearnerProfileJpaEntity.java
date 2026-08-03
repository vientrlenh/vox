package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

@Entity
@Table(
    name = "learner_profile",
    indexes = @Index(
        name = "idx_learner_profile_student_version",
        columnList = "student_id, version",
        unique = true
    )
)
public class LearnerProfileJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Column(name = "goal_type", length = 24)
    private String goalType;

    @Column(name = "target_exam", length = 24)
    private String targetExam;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "flsa_score", precision = 5, scale = 2)
    private BigDecimal flsaScore;

    @Column(name = "flsa_raw_answers_json", columnDefinition = "TEXT")
    private String flsaRawAnswersJson;

    @Column(name = "auto_update_interest", nullable = false)
    private boolean autoUpdateInterest = true;

    @Column(name = "quiz_completed_at")
    private Instant quizCompletedAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected LearnerProfileJpaEntity() {
    }

    public LearnerProfileJpaEntity(
            UUID studentId,
            int version,
            String goalType,
            String targetExam,
            LocalDate targetDate,
            BigDecimal flsaScore,
            String flsaRawAnswersJson,
            boolean autoUpdateInterest,
            Instant quizCompletedAt,
            Instant recordedAt) {
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

    public UUID getStudentId() {
        return studentId;
    }

    public int getVersion() {
        return version;
    }

    public String getGoalType() {
        return goalType;
    }

    public String getTargetExam() {
        return targetExam;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public BigDecimal getFlsaScore() {
        return flsaScore;
    }

    public String getFlsaRawAnswersJson() {
        return flsaRawAnswersJson;
    }

    public boolean isAutoUpdateInterest() {
        return autoUpdateInterest;
    }

    public Instant getQuizCompletedAt() {
        return quizCompletedAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
