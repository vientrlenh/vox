package com.sep.vox.domain.model.gradelevel;

import com.sep.vox.domain.model.BaseModel;

public class GradeLevel extends BaseModel {
    private String code;
    private String name;
    private String description;
    private int order;
    private GradeLevelStatus status;

    public GradeLevel() {}

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

    public GradeLevelStatus getStatus() {
        return status;
    }

    public void setStatus(GradeLevelStatus status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<GradeLevel, Builder> {

        private Builder() {
            super(new GradeLevel());
        }

        @Override
        protected Builder self() {
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

        public Builder status(GradeLevelStatus status) {
            target.status = status;
            return this;
        }
    }
}
