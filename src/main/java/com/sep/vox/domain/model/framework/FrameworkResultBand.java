package com.sep.vox.domain.model.framework;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;

public class FrameworkResultBand {
    private UUID id;
    private UUID frameworkVersionId;
    private Code code;
    private String label;
    private String description;
    private PositiveInteger order;

    protected FrameworkResultBand() {}

    public FrameworkResultBand(UUID id, UUID frameworkVersionId, Code code, String label, String description, PositiveInteger order, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.frameworkVersionId = frameworkVersionId;
        this.code = code;
        this.label = label;
        this.description = description;
        this.order = order;
    }

    public FrameworkResultBand(Builder builder) {
        this.id = builder.id;
        this.frameworkVersionId = builder.frameworkVersionId;
        this.code = builder.code;
        this.label = builder.label;
        this.description = builder.description;
        this.order = builder.order;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public PositiveInteger getOrder() {
        return order;
    }

    public void setOrder(PositiveInteger order) {
        this.order = order;
    }

    public static Builder builder() {
        return new FrameworkResultBand.Builder();
    }

    public static FrameworkResultBand create(UUID frameworkVersionId, String code, String label, String description, Integer order) {
        return FrameworkResultBand.builder()
            .frameworkVersionId(frameworkVersionId)
            .code(Code.from(code))
            .label(label)
            .description(description)
            .order(PositiveInteger.from(order))
            .build();
    }

    public static class Builder {
        private UUID id;
        private UUID frameworkVersionId;
        private Code code;
        private String label;
        private String description;
        private PositiveInteger order;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder frameworkVersionId(UUID frameworkVersionId) {
            this.frameworkVersionId = frameworkVersionId;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder order(PositiveInteger order) {
            this.order = order;
            return this;
        }

        public FrameworkResultBand build() {
            return new FrameworkResultBand(this);
        }
    }
}
