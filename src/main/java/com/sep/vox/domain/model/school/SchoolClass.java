package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class SchoolClass extends BaseModel {
    private UUID schoolId;
    private UUID learningLanguageId;
    private UUID schoolGradeId;
    private String name;
    private String description;
    private SchoolClassStatus status;
    private Instant updatedAt;
    private Instant archivedAt;
    private UUID createdBy;

    public SchoolClass() {}

    public SchoolClass(UUID schoolId, UUID learningLanguageId, UUID schoolGradeId, String name, String description, SchoolClassStatus status, Instant updatedAt, Instant archivedAt, UUID createdBy) {
        this.schoolId = schoolId;
        this.learningLanguageId = learningLanguageId;
        this.schoolGradeId = schoolGradeId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.updatedAt = updatedAt;
        this.archivedAt = archivedAt;
        this.createdBy = createdBy;
    }

    public SchoolClass(Builder builder) {
        super(builder.id, builder.createdAt);
        this.schoolId = builder.schoolId;
        this.learningLanguageId = builder.learningLanguageId;
        this.schoolGradeId = builder.schoolGradeId;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.archivedAt = builder.archivedAt;
        this.createdBy = builder.createdBy;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getLearningLanguageId() {
        return learningLanguageId;
    }

    public void setLearningLanguageId(UUID learningLanguageId) {
        this.learningLanguageId = learningLanguageId;
    }

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
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

    public SchoolClassStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolClassStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public static Builder builder() {
        return new SchoolClass.Builder();
    }

    public static SchoolClass create(UUID schoolId, UUID learningLanguageId, UUID schoolGradeId, String name,
            String description, UUID createdBy) {
        return SchoolClass.builder()
                .schoolId(schoolId)
                .learningLanguageId(learningLanguageId)
                .schoolGradeId(schoolGradeId)
                .name(name)
                .description(description)
                .status(SchoolClassStatus.ACTIVE)
                .createdBy(createdBy)
                .build();
    }

    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private UUID learningLanguageId;
        private UUID schoolGradeId;
        private String name;
        private String description;
        private SchoolClassStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant archivedAt;
        private UUID createdBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
            return this;
        }

        public Builder learningLanguageId(UUID learningLanguageId) {
            this.learningLanguageId = learningLanguageId;
            return this;
        }

        public Builder schoolGradeId(UUID schoolGradeId) {
            this.schoolGradeId = schoolGradeId;
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

        public Builder status(SchoolClassStatus status) {
            this.status = status;
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

        public Builder archivedAt(Instant archivedAt) {
            this.archivedAt = archivedAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public SchoolClass build() {
            return new SchoolClass(this);
        }
    }
}
