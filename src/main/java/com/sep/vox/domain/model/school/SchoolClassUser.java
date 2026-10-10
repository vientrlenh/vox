package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

public class SchoolClassUser {
    private UUID id;
    private UUID userId;
    private UUID schoolClassId;
    private Instant joinAt;
    private Instant leftAt;
    private UUID assignedBy;

    protected SchoolClassUser() {}

    public SchoolClassUser(UUID id, UUID userId, UUID schoolClassId, Instant joinAt,
            Instant leftAt, UUID assignedBy) {
        this.id = id;
        this.userId = userId;
        this.schoolClassId = schoolClassId;
        this.joinAt = joinAt;
        this.leftAt = leftAt;
        this.assignedBy = assignedBy;
    }

    public SchoolClassUser(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.schoolClassId = builder.schoolClassId;
        this.joinAt = builder.joinAt;
        this.leftAt = builder.leftAt;
        this.assignedBy = builder.assignedBy;
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

    public Instant getJoinAt() {
        return joinAt;
    }

    public void setJoinAt(Instant joinAt) {
        this.joinAt = joinAt;
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

    public static Builder builder() {
        return new SchoolClassUser.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private UUID schoolClassId;
        private Instant joinAt;
        private Instant leftAt;
        private UUID assignedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder schoolClassId(UUID schoolClassId) {
            this.schoolClassId = schoolClassId;
            return this;
        }

        public Builder joinAt(Instant joinAt) {
            this.joinAt = joinAt;
            return this;
        }

        public Builder leftAt(Instant leftAt) {
            this.leftAt = leftAt;
            return this;
        }

        public Builder assignedBy(UUID assignedBy) {
            this.assignedBy = assignedBy;
            return this;
        }

        public SchoolClassUser build() {
            return new SchoolClassUser(this);
        }
    }
    
}
