package com.sep.vox.domain.model.framework;

import com.sep.vox.domain.model.BaseModel;
import java.util.UUID;

public class FrameworkResultBand extends BaseModel {
    private UUID frameworkVersionId;
    private String code;
    private String label;
    private String description;
    private int order;

    public FrameworkResultBand() {}

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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public static FrameworkResultBand forVersion(UUID frameworkVersionId, String code, String label,
                                                 String description, int order) {
        return builder()
                .frameworkVersionId(frameworkVersionId)
                .code(code)
                .label(label)
                .description(description)
                .order(order)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<FrameworkResultBand, Builder> {

        private Builder() {
            super(new FrameworkResultBand());
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

        public Builder label(String label) {
            target.label = label;
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
