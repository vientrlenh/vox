package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "student_question_exposures",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_question_exposure_student_question",
        columnNames = {"student_id", "practice_question_id"}
    ),
    indexes = @Index(
        name = "idx_student_question_exposure_student_seen",
        columnList = "student_id, seen_at"
    )
)
public class StudentQuestionExposureJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "practice_question_id", nullable = false)
    private UUID practiceQuestionId;

    @Column(name = "seen_at", nullable = false)
    private Instant seenAt;

    protected StudentQuestionExposureJpaEntity() {
    }

    public StudentQuestionExposureJpaEntity(
            UUID id,
            UUID studentId,
            UUID practiceQuestionId,
            Instant seenAt) {
        this.id = id;
        this.studentId = studentId;
        this.practiceQuestionId = practiceQuestionId;
        this.seenAt = seenAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getPracticeQuestionId() {
        return practiceQuestionId;
    }

    public Instant getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(Instant seenAt) {
        this.seenAt = seenAt;
    }
}
