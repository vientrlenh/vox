package com.sep.vox.domain.model.framework;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveLong;

public class FrameworkVersion extends BaseModel {
    private UUID frameworkId;
    private Code code;
    private String name;
    private String description;
    private PositiveLong version;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private FrameworkVersionStatus status;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected FrameworkVersion() {}

    public FrameworkVersion(UUID frameworkId, Code code, String name, String description, PositiveLong version,
            Instant effectiveFrom, Instant effectiveTo, FrameworkVersionStatus status,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.frameworkId = frameworkId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = status;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public FrameworkVersion(Builder builder) {
        super(builder.id, builder.createdAt);
        this.frameworkId = builder.frameworkId;
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.version = builder.version;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveTo = builder.effectiveTo;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }

    public UUID getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(UUID frameworkId) {
        this.frameworkId = frameworkId;
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

    public PositiveLong getVersion() {
        return version;
    }

    public void setVersion(PositiveLong version) {
        this.version = version;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
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

    public FrameworkVersionStatus getStatus() {
        return status;
    }

    public void setStatus(FrameworkVersionStatus status) {
        this.status = status;
    }

    public static Builder builder() {
        return new FrameworkVersion.Builder();
    }

    public static FrameworkVersion createDraft(UUID frameworkId, String code, String name, String description, Long version, Instant effectiveFrom, Instant effectiveTo, UUID createdBy) {
        return FrameworkVersion.builder()
            .frameworkId(frameworkId)
            .code(Code.from(code))
            .name(name)
            .description(description)
            .version(PositiveLong.from(version))
            .effectiveFrom(effectiveFrom)
            .effectiveTo(effectiveTo)
            .status(FrameworkVersionStatus.DRAFT)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }

    public static class Builder {
        private UUID id;
        private UUID frameworkId;
        private Code code;
        private String name;
        private String description;
        private PositiveLong version;
        private Instant effectiveFrom;
        private Instant effectiveTo;
        private FrameworkVersionStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder frameworkId(UUID frameworkId) {
            this.frameworkId = frameworkId;
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

        public Builder version(PositiveLong version) {
            this.version = version;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public Builder status(FrameworkVersionStatus status) {
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

        public FrameworkVersion build() {
            return new FrameworkVersion(this);
        }
    }
}
