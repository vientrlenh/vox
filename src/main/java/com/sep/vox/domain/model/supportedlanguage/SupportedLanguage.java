package com.sep.vox.domain.model.supportedlanguage;

import com.sep.vox.domain.model.BaseModel;
import com.sep.vox.domain.valueobject.LanguageCode;

public class SupportedLanguage extends BaseModel {
    private LanguageCode code;
    private String name;
    private String description;
    private boolean isActive;

    public SupportedLanguage() {
    }

    public LanguageCode getCode() {
        return code;
    }

    public void setCode(LanguageCode code) {
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

    public static class Builder extends BaseModel.Builder<SupportedLanguage, Builder> {

        private Builder() {
            super(new SupportedLanguage());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder code(LanguageCode code) {
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
