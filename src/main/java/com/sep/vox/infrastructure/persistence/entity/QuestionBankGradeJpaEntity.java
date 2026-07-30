package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_bank_grades", indexes = {
    @Index(columnList = "question_bank_id, school_grade_id", name = "idx_question_bank_grades_bank_grade", unique = true),
    @Index(columnList = "school_grade_id", name = "idx_question_bank_grades_school_grade")
})
public class QuestionBankGradeJpaEntity {

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

    @Column(name = "question_bank_id", nullable = false, updatable = false)
    private UUID questionBankId;

    @Column(name = "school_grade_id", nullable = false, updatable = false)
    private UUID schoolGradeId;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private Instant attachedAt;

    @Column(name = "attached_by", updatable = false)
    private UUID attachedBy;

    protected QuestionBankGradeJpaEntity() {}

    public QuestionBankGradeJpaEntity(UUID id, UUID questionBankId, UUID schoolGradeId, Instant attachedAt,
            UUID attachedBy) {
        this.id = id;
        this.questionBankId = questionBankId;
        this.schoolGradeId = schoolGradeId;
        this.attachedAt = attachedAt;
        this.attachedBy = attachedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(UUID questionBankId) {
        this.questionBankId = questionBankId;
    }

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }

    public Instant getAttachedAt() {
        return attachedAt;
    }

    public void setAttachedAt(Instant attachedAt) {
        this.attachedAt = attachedAt;
    }

    public UUID getAttachedBy() {
        return attachedBy;
    }

    public void setAttachedBy(UUID attachedBy) {
        this.attachedBy = attachedBy;
    }
}
