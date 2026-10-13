package com.sep.vox.domain.model.question;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class QuestionBank extends BaseModel {
    private UUID learningLanguageId; 
    private UUID schoolId;
    private String name;
    private String description;
    private QuestionBankOwnerType ownerType;
    private QuestionBankStatus status;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected QuestionBank() {}

    public QuestionBank(UUID learningLanguageId, UUID schoolId, String name, String description, QuestionBankOwnerType ownerType, QuestionBankStatus status,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.learningLanguageId = learningLanguageId;
        this.schoolId = schoolId;
        this.name = name;
        this.description = description;
        this.ownerType = ownerType;
        this.status = status;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public QuestionBank(Builder builder) {
        super(builder.id, builder.createdAt);
        this.learningLanguageId = builder.learningLanguageId;
        this.schoolId = builder.schoolId;
        this.name = builder.name;
        this.description = builder.description;
        this.ownerType = builder.ownerType;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }

    public UUID getLearningLanguageId() {
        return learningLanguageId;
    }
    
    public void setLearningLanguageId(UUID learningLanguageId) {
        this.learningLanguageId = learningLanguageId;
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

    public QuestionBankStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionBankStatus status) {
        this.status = status;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public QuestionBankOwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(QuestionBankOwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public static Builder builder() {
        return new QuestionBank.Builder();
    }

    public static QuestionBank create(UUID languageId, UUID schoolId, String code, String name, String description, QuestionBankOwnerType ownerType, 
    Instant now, UUID createdBy) {
        return QuestionBank.builder()
        
    }

    public static class Builder {
        private UUID id;
        private UUID learningLanguageId; 
        private UUID schoolId;
        private String name;
        private String description;
        private QuestionBankOwnerType ownerType;
        private QuestionBankStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder learningLanguageId(UUID learningLanguageId) {
            this.learningLanguageId = learningLanguageId;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            this.schoolId = schoolId;
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

        public Builder ownerType(QuestionBankOwnerType ownerType) {
            this.ownerType = ownerType;
            return this;
        }

        public Builder status(QuestionBankStatus status) {
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

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public QuestionBank build() {
            return new QuestionBank(this);
        }
    }
}
