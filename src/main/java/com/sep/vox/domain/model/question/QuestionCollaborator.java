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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final QuestionCollaborator target = new QuestionCollaborator();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            target.userId = userId;
            return this;
        }

        public Builder questionId(UUID questionId) {
            target.questionId = questionId;
            return this;
        }

        public Builder permission(QuestionCollaboratorPermission permission) {
            target.permission = permission;
            return this;
        }

        public Builder assignedAt(Instant assignedAt) {
            target.assignedAt = assignedAt;
            return this;
        }

        public QuestionCollaborator build() {
            return target;
        }
    }
}
