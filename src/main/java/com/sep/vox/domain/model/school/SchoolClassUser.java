package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

public class SchoolClassUser {
    private UUID id;
    private UUID userId;
    private UUID schoolClassId;
    private boolean isActive;
    private Instant joinedAt;
    private Instant leftAt;
    private UUID assignedBy;

    public SchoolClassUser() {
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

    public UUID getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(UUID schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Instant leftAt) {
        this.leftAt = leftAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public void deactivate(Instant leftAt) {
        this.isActive = false;
        this.leftAt = leftAt;
    }

    public void activate() {
        this.isActive = true;
        this.leftAt = null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SchoolClassUser target = new SchoolClassUser();

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

        public Builder schoolClassId(UUID schoolClassId) {
            target.schoolClassId = schoolClassId;
            return this;
        }

        public Builder active(boolean isActive) {
            target.isActive = isActive;
            return this;
        }

        public Builder joinedAt(Instant joinedAt) {
            target.joinedAt = joinedAt;
            return this;
        }

        public Builder leftAt(Instant leftAt) {
            target.leftAt = leftAt;
            return this;
        }

        public Builder assignedBy(UUID assignedBy) {
            target.assignedBy = assignedBy;
            return this;
        }

        public SchoolClassUser build() {
            return target;
        }
    }
}
