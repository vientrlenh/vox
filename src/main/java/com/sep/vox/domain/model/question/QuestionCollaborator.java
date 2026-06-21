package com.sep.vox.domain.model.question;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QuestionCollaborator {
    private UUID id;
    private UUID userId;
    private UUID questionId;
    private QuestionCollaboratorPermission permission;
    private OffsetDateTime assignedAt;

    public QuestionCollaborator() {}

    public QuestionCollaborator(UUID id, UUID userId, UUID questionId, QuestionCollaboratorPermission permission,
            OffsetDateTime assignedAt) {
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

    public QuestionCollaboratorPermission getPermission() {
        return permission;
    }

    public void setPermission(QuestionCollaboratorPermission permission) {
        this.permission = permission;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    
}
