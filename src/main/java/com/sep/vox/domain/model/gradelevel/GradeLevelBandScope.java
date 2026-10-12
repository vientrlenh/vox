package com.sep.vox.domain.model.gradelevel;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class GradeLevelBandScope extends BaseModel {
    private UUID gradeLevelId;
    private UUID frameworkVersionId;
    private UUID defaultTargetBandId;
    private UUID hardMaxBandId;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected GradeLevelBandScope() {}

    public GradeLevelBandScope(UUID gradeLevelId, UUID frameworkVersionId, UUID defaultTargetBandId,
            UUID hardMaxBandId, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.gradeLevelId = gradeLevelId;
        this.frameworkVersionId = frameworkVersionId;
        this.defaultTargetBandId = defaultTargetBandId;
        this.hardMaxBandId = hardMaxBandId;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public GradeLevelBandScope(Builder builder) {
        super(builder.id, builder.createdAt);
        this.gradeLevelId = builder.gradeLevelId;
        this.frameworkVersionId = builder.frameworkVersionId;
        this.defaultTargetBandId = builder.defaultTargetBandId;
        this.hardMaxBandId = builder.hardMaxBandId;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
    }

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public UUID getDefaultTargetBandId() {
        return defaultTargetBandId;
    }

    public void setDefaultTargetBandId(UUID defaultTargetBandId) {
        this.defaultTargetBandId = defaultTargetBandId;
    }

    public UUID getHardMaxBandId() {
        return hardMaxBandId;
    }

    public void setHardMaxBandId(UUID hardMaxBandId) {
        this.hardMaxBandId = hardMaxBandId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public static Builder builder() {
        return new GradeLevelBandScope.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID gradeLevelId;
        private UUID frameworkVersionId;
        private UUID defaultTargetBandId;
        private UUID hardMaxBandId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder gradeLevelId(UUID gradeLevelId) {
            this.gradeLevelId = gradeLevelId;
            return this;
        }

        public Builder frameworkVersionId(UUID frameworkVersionId) {
            this.frameworkVersionId = frameworkVersionId;
            return this;
        }

        public Builder defaultTargetBandId(UUID defaultTargetBandId) {
            this.defaultTargetBandId = defaultTargetBandId;
            return this;
        }

        public Builder hardMaxBandId(UUID hardMaxBandId) {
            this.hardMaxBandId = hardMaxBandId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public GradeLevelBandScope build() {
            return new GradeLevelBandScope(this);
        }
    }
}
