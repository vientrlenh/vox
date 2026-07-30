package com.sep.vox.domain.model.question;

import java.time.Instant;
import java.util.UUID;

public class QuestionCollaborator {
    private UUID id;
    private UUID userId;
    private UUID questionId;
    private QuestionCollaboratorPermission permission;
    private Instant assignedAt;

    public QuestionCollaborator() {}

    public QuestionCollaborator(UUID id, UUID userId, UUID questionId, QuestionCollaboratorPermission permission,
            Instant assignedAt) {
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

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    
}
