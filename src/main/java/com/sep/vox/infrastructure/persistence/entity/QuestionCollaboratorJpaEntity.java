package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_collaborators", indexes = {
    @Index(columnList = "question_id, user_id", name = "idx_question_collaborators_question_user", unique = true),
    @Index(columnList = "user_id", name = "idx_question_collaborators_user")
})
public class QuestionCollaboratorJpaEntity {

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

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "permission", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_question_collaborators_permission_valid",
            constraint = "permission IN ('READ_ONLY', 'CAN_USE', 'CAN_EDIT')"
        )
    })
    private String permission;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    protected QuestionCollaboratorJpaEntity() {
    }

    public QuestionCollaboratorJpaEntity(UUID id, UUID userId, UUID questionId, String permission, OffsetDateTime assignedAt) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.permission = permission;
        this.assignedAt = assignedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
