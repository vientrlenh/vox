package com.sep.vox.domain.model.framework;

import com.sep.vox.domain.model.BaseModel;
import java.time.Instant;
import java.util.UUID;

public class FrameworkVersion extends BaseModel {
    private UUID frameworkId;
    private String code;
    private String name;
    private String description;
    private int version;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private FrameworkVersionStatus status;

    public FrameworkVersion() {}

    public UUID getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(UUID frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
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

    public FrameworkVersionStatus getStatus() {
        return status;
    }

    public void setStatus(FrameworkVersionStatus status) {
        this.status = status;
    }

    public static FrameworkVersion draft(UUID frameworkId, String code, String name, String description,
                                         int version, Instant effectiveFrom, Instant effectiveTo) {
        return builder()
                .frameworkId(frameworkId)
                .code(code)
                .name(name)
                .description(description)
                .version(version)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .status(FrameworkVersionStatus.DRAFT)
                .build();
    }

    public FrameworkVersion withEditableFields(String newCode, String newName, String newDescription,
                                               Instant newEffectiveFrom, Instant newEffectiveTo) {
        return builder()
                .id(getId())
                .frameworkId(frameworkId)
                .code(newCode)
                .name(newName)
                .description(newDescription)
                .version(version)
                .effectiveFrom(newEffectiveFrom)
                .effectiveTo(newEffectiveTo)
                .status(status)
                .createdAt(getCreatedAt())
                .createdBy(getCreatedBy())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<FrameworkVersion, Builder> {

        private Builder() {
            super(new FrameworkVersion());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder frameworkId(UUID frameworkId) {
            target.frameworkId = frameworkId;
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder version(int version) {
            target.version = version;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            target.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            target.effectiveTo = effectiveTo;
            return this;
        }

        public Builder status(FrameworkVersionStatus status) {
            target.status = status;
            return this;
        }
    }
}
