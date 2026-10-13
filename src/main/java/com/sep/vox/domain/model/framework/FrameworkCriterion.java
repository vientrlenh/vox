package com.sep.vox.domain.model.framework;

import java.util.UUID;

import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;

public class FrameworkCriterion {
    private UUID id;
    private UUID frameworkVersionId;
    private Code code;
    private String name;
    private String description;
    private PositiveInteger order;

    protected FrameworkCriterion() {}

    public FrameworkCriterion(UUID id, UUID frameworkVersionId, Code code, String name, String description, PositiveInteger order) {
        this.id = id;
        this.frameworkVersionId = frameworkVersionId;
        this.code = code;
        this.name = name;
        this.description = description; 
        this.order = order;
    }

    public FrameworkCriterion(Builder builder) {
        this.id = builder.id;
        this.frameworkVersionId = builder.frameworkVersionId;
        this.code = builder.code;
        this.name = builder.name;
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

    public PositiveInteger getOrder() {
        return order;
    }

    public void setOrder(PositiveInteger order) {
        this.order = order;
    }

    public static Builder builder() {
        return new FrameworkCriterion.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID frameworkVersionId;
        private Code code;
        private String name;
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

        public Builder name(String name) {
            this.name = name;
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

        public FrameworkCriterion build() {
            return new FrameworkCriterion(this);
        }
    }
}
