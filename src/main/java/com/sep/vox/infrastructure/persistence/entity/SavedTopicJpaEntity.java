package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "saved_topic",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_saved_topic_student_topic",
        columnNames = {"student_id", "practice_topic_id"}
    ),
    indexes = @Index(name = "idx_saved_topic_student", columnList = "student_id")
)
public class SavedTopicJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "practice_topic_id", nullable = false, updatable = false)
    private UUID practiceTopicId;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private OffsetDateTime savedAt;

    protected SavedTopicJpaEntity() {
    }

    public SavedTopicJpaEntity(
            UUID id,
            UUID studentId,
            UUID practiceTopicId,
            OffsetDateTime savedAt) {
        this.id = id;
        this.studentId = studentId;
        this.practiceTopicId = practiceTopicId;
        this.savedAt = savedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getPracticeTopicId() {
        return practiceTopicId;
    }

    public OffsetDateTime getSavedAt() {
        return savedAt;
    }
}
