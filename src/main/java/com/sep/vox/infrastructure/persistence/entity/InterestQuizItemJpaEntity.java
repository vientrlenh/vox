package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "interest_quiz_item")
public class InterestQuizItemJpaEntity {

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

    @Column(name = "dimensions_json", nullable = false, columnDefinition = "TEXT")
    private String dimensionsJson;

    @Column(name = "statements_json", nullable = false, columnDefinition = "TEXT")
    private String statementsJson;

    // TEXT: chữ do LLM sinh, không có trần độ dài -- xem chú thích ở V15__personalize.sql
    // (mục 18. interest_quiz_item).
    @Column(name = "desirability_note", columnDefinition = "TEXT")
    private String desirabilityNote;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // NULL = bộ gốc/tĩnh dùng chung (fallback); có giá trị = sinh riêng cho đúng học sinh này
    // (gói 13 "sinh quiz theo tình huống", xem task/implement/13-...).
    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InterestQuizItemJpaEntity() {
    }

    public InterestQuizItemJpaEntity(
            String dimensionsJson,
            String statementsJson,
            String desirabilityNote,
            boolean active,
            UUID studentId,
            Instant createdAt) {
        this.dimensionsJson = dimensionsJson;
        this.statementsJson = statementsJson;
        this.desirabilityNote = desirabilityNote;
        this.active = active;
        this.studentId = studentId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getDimensionsJson() {
        return dimensionsJson;
    }

    public String getStatementsJson() {
        return statementsJson;
    }

    public String getDesirabilityNote() {
        return desirabilityNote;
    }

    public boolean isActive() {
        return active;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
