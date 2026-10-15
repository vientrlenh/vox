package com.sep.vox.domain.model.language;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.Code;

public class LearningLanguage extends BaseModel {
    private Code code;
    private String name;
    private String description;
    private LearningLanguageStatus status;
    private Instant availableAt;
    private Instant unavailableAt;
    private UUID updatedBy;

    protected LearningLanguage() {}

    public LearningLanguage(Code code, String name, String description, LearningLanguageStatus status,
            Instant availableAt, Instant unavailableAt, UUID updatedBy) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status;
        this.availableAt = availableAt;
        this.unavailableAt = unavailableAt;
        this.updatedBy = updatedBy;
    }

    public LearningLanguage(Builder builder) {
        super(builder.id, builder.createdAt);
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.availableAt = builder.availableAt;
        this.unavailableAt = builder.unavailableAt;
        this.updatedBy = builder.updatedBy;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
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

    public LearningLanguageStatus getStatus() {
        return status;
    }

    public void setStatus(LearningLanguageStatus status) {
        this.status = status;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public Instant getUnavailableAt() {
        return unavailableAt;
    }

    public void setUnavailableAt(Instant unavailableAt) {
        this.unavailableAt = unavailableAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public static Builder builder() {
        return new LearningLanguage.Builder();
    }

    
    public static class Builder {
        private UUID id;
        private Code code;
        private String name;
        private String description;
        private LearningLanguageStatus status;
        private Instant createdAt;
        private Instant availableAt;
        private Instant unavailableAt;
        private UUID updatedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
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

        public Builder status(LearningLanguageStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder availableAt(Instant availableAt) {
            this.availableAt = availableAt;
            return this;
        }

        public Builder unavailableAt(Instant unavailableAt) {
            this.unavailableAt = unavailableAt;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public LearningLanguage build() {
            return new LearningLanguage(this);
        }
    }
}
