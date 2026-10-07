package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.common.BaseModel;
import com.sep.vox.domain.model.language.LearningLanguage;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;

public class SchoolClass extends BaseModel {
    private UUID schoolId;
    private LearningLanguage language;
    private UUID schoolGradeId;
    private Code code;
    private Name name;
    private String description;
    private SchoolClassStatus status;
    private Instant updatedAt;
    private Instant archivedAt;
    private UUID createdBy;

    public SchoolClass() {}

    public SchoolClass(UUID schoolId, LearningLanguage language, UUID schoolGradeId, Code code, Name name, String description, SchoolClassStatus status, Instant updatedAt, Instant archivedAt, UUID createdBy) {
        this.schoolId = schoolId;
        this.language = language;
        this.schoolGradeId = schoolGradeId;
        this.code = code;
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
        this.language = builder.language;
        this.schoolGradeId = builder.schoolGradeId;
        this.code = builder.code;
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

    public LearningLanguage getLanguage() {
        return language;
    }

    public void setLanguage(LearningLanguage language) {
        this.language = language;
    }

    public UUID getSchoolGradeId() {
        return schoolGradeId;
    }

    public void setSchoolGradeId(UUID schoolGradeId) {
        this.schoolGradeId = schoolGradeId;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
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

    public static SchoolClass create(UUID schoolId, LearningLanguage language, UUID schoolGradeId, String code, String name,
            String description, UUID createdBy) {
        return SchoolClass.builder()
                .schoolId(schoolId)
                .language(language)
                .schoolGradeId(schoolGradeId)
                .code(Code.from(code))
                .name(Name.from(name))
                .description(description)
                .status(SchoolClassStatus.ACTIVE)
                .createdBy(createdBy)
                .build();
    }

    public static class Builder {
        private UUID id;
        private UUID schoolId;
        private LearningLanguage language;
        private UUID schoolGradeId;
        private Code code;
        private Name name;
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

        public Builder language(LearningLanguage language) {
            this.language = language;
            return this;
        }

        public Builder schoolGradeId(UUID schoolGradeId) {
            this.schoolGradeId = schoolGradeId;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        public Builder name(Name name) {
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
