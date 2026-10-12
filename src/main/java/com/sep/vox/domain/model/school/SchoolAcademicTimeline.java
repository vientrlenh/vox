package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class SchoolAcademicTimeline extends BaseModel {
    private String name;
    private Instant startAt;
    private Instant endAt;
    private UUID createdBy;

    protected SchoolAcademicTimeline() {}

    public SchoolAcademicTimeline(String name, Instant startAt, Instant endAt, UUID createdBy) {
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdBy = createdBy;
    }

    public SchoolAcademicTimeline(Builder builder) {
        super(builder.id, builder.createdAt);
        this.name = builder.name;
        this.startAt = builder.startAt;
        this.endAt = builder.endAt;
        this.createdBy = builder.createdBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public static Builder builder() {
        return new SchoolAcademicTimeline.Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private Instant startAt;
        private Instant endAt;
        private Instant createdAt;
        private UUID createdBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder startAt(Instant startAt) {
            this.startAt = startAt;
            return this;
        }

        public Builder endAt(Instant endAt) {
            this.endAt = endAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public SchoolAcademicTimeline build() {
            return new SchoolAcademicTimeline(this);
        }
    }
}
