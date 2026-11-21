package com.sep.vox.domain.model.framework;

import com.sep.vox.domain.model.BaseModel;

import com.sep.vox.domain.valueobject.FrameworkCode;

public class Framework extends BaseModel {
    private FrameworkCode code;
    private String name;
    private String description;
    private boolean isActive;

    public Framework() {}

    public FrameworkCode getCode() {
        return code;
    }

    public void setCode(FrameworkCode code) {
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<Framework, Builder> {

        private Builder() {
            super(new Framework());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder code(FrameworkCode code) {
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

        public Builder active(boolean isActive) {
            target.isActive = isActive;
            return this;
        }
    }
}
