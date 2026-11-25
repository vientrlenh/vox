package com.sep.vox.domain.model.framework;

import com.sep.vox.domain.model.BaseModel;
import java.util.UUID;

public class FrameworkCriterion extends BaseModel {
    private UUID frameworkVersionId;
    private String code;
    private String name;
    private String description;
    private int order;

    public FrameworkCriterion() {}

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public static FrameworkCriterion forVersion(UUID frameworkVersionId, String code, String name,
                                                String description, int order) {
        return builder()
                .frameworkVersionId(frameworkVersionId)
                .code(code)
                .name(name)
                .description(description)
                .order(order)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<FrameworkCriterion, Builder> {

        private Builder() {
            super(new FrameworkCriterion());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder frameworkVersionId(UUID frameworkVersionId) {
            target.frameworkVersionId = frameworkVersionId;
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

        public Builder order(int order) {
            target.order = order;
            return this;
        }
    }
}
