package com.sep.vox.domain.model.rubric;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;


public class Rubric extends BaseModel {
    private UUID learningLanguageId;
    private UUID frameworkId;
    private String name;
    private String description;
    private RubricOwnerType ownerType;
    private UUID schoolId;

    protected Rubric() {}

    public Rubric(UUID learningLanguageId, UUID frameworkId, String name, String description,
            RubricOwnerType ownerType, UUID schoolId) {
        this.learningLanguageId = learningLanguageId;
        this.frameworkId = frameworkId;
        this.name = name;
        this.description = description;
        this.ownerType = ownerType;
        this.schoolId = schoolId;
    }

    public Rubric(Builder builder) {
        super(builder.id, builder.createdAt);
        this.learningLanguageId = builder.learningLanguageId;
        this.frameworkId = builder.frameworkId;
        this.name = builder.name;
        this.description = builder.description;
        this.ownerType = builder.ownerType;
        this.schoolId = builder.schoolId;
    }

    public UUID getLearningLanguageId() {
        return learningLanguageId;
    }

    public void setLearningLanguageId(UUID learningLanguageId) {
        this.learningLanguageId = learningLanguageId;
    }

    public UUID getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(UUID frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RubricOwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(RubricOwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public static Builder builder() {
        return new Rubric.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID learningLanguageId;
        private UUID frameworkId;
        private String name;
        private String description;
        private RubricOwnerType ownerType;
        private UUID schoolId;
        private Instant createdAt;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder learningLanguageId(UUID learningLanguageId) {
            this.learningLanguageId = learningLanguageId;
            return this;
        }

        public Builder frameworkId(UUID frameworkId) {
            this.frameworkId = frameworkId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder ownerType(RubricOwnerType ownerType) {
            this.ownerType = ownerType;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Rubric build() {
            return new Rubric(this);
        }
    }
}
